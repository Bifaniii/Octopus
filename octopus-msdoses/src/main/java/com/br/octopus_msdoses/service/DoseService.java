package com.br.octopus_msdoses.service;

import com.br.octopus_msdoses.domain.Dose;
import com.br.octopus_msdoses.domain.enums.StatusDose;
import com.br.octopus_msdoses.dto.request.AgendamentoRequest;
import com.br.octopus_msdoses.dto.response.DoseResponse;
import com.br.octopus_msdoses.exception.RecursoNaoEncontradoException;
import com.br.octopus_msdoses.repository.DoseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoseService {

    private final DoseRepository doseRepository;
    private final Clock clock;

    /**
     * Uma dose por horário de cada item. Idempotente: se a prescrição já tem doses, devolve as que existem, para
     * que uma nova tentativa do octopus-msprescricao (depois de um timeout, por exemplo) não duplique a agenda.
     */
    @Transactional
    public List<DoseResponse> agendar(AgendamentoRequest request) {
        List<Dose> existentes = doseRepository.findByPrescricaoIdOrderByHorarioPrevistoAsc(request.prescricaoId());
        if (!existentes.isEmpty()) {
            return existentes.stream().map(DoseResponse::from).toList();
        }

        List<Dose> novas = new ArrayList<>();
        for (AgendamentoRequest.Item item : request.itens()) {
            item.horarios().stream().distinct().forEach(horario -> novas.add(Dose.agendar(request.prescricaoId(),
                    item.itemPrescricaoId(), request.internacaoId(), item.medicacaoId(), horario)));
        }
        return doseRepository.saveAll(novas).stream()
                .sorted((a, b) -> a.getHorarioPrevisto().compareTo(b.getHorarioPrevisto()))
                .map(DoseResponse::from)
                .toList();
    }

    /** A prescrição foi desativada: as pendentes viram CANCELADA, as aplicadas ficam como estão. Idempotente. */
    @Transactional
    public List<DoseResponse> cancelarDaPrescricao(Long prescricaoId) {
        List<Dose> doses = doseRepository.findByPrescricaoIdOrderByHorarioPrevistoAsc(prescricaoId);
        doses.forEach(Dose::cancelar);
        return doses.stream().map(DoseResponse::from).toList();
    }

    /** Filtros opcionais; o período ({@code de} e {@code ate}, inclusivos) compara a data do horário previsto. */
    @Transactional(readOnly = true)
    public List<DoseResponse> listar(Long internacaoId, Long prescricaoId, StatusDose status, LocalDate de,
                                     LocalDate ate) {
        LocalDateTime inicio = de == null ? LocalDateTime.of(1900, 1, 1, 0, 0) : de.atStartOfDay();
        LocalDateTime fim = ate == null ? LocalDateTime.of(9999, 1, 1, 0, 0) : ate.plusDays(1).atStartOfDay();
        return doseRepository.listar(internacaoId, prescricaoId, status, inicio, fim).stream()
                .map(DoseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoseResponse buscar(Long id) {
        return DoseResponse.from(obter(id));
    }

    /** RN-05: grava o auxiliar (dono do token) e a hora da clínica. Não há edição depois. */
    @Transactional
    public DoseResponse aplicar(Long id, String auxiliarEmail, String observacao) {
        Dose dose = obter(id);
        dose.aplicar(auxiliarEmail, LocalDateTime.now(clock), observacao);
        return DoseResponse.from(dose);
    }

    private Dose obter(Long id) {
        return doseRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dose não encontrada com o id: " + id));
    }
}
