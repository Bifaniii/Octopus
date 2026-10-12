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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoseService {

    private final DoseRepository doseRepository;
    private final Estoque estoque;
    private final TransactionTemplate transacao;
    private final Clock clock;

    /**
     * Uma dose por horário de cada item. Idempotente: se a prescrição já tem doses, devolve as que existem, para
     * que uma nova tentativa do octopus-msprescricao (depois de um timeout, por exemplo) não duplique a agenda.
     * Cada dose nova separa 1 unidade do estoque do medicamento. A reserva vem antes de gravar, fora da transação;
     * se a gravação falhar, o que foi reservado volta.
     */
    public List<DoseResponse> agendar(AgendamentoRequest request) {
        List<Dose> existentes = transacao.execute(status ->
                doseRepository.findByPrescricaoIdOrderByHorarioPrevistoAsc(request.prescricaoId()));
        if (!existentes.isEmpty()) {
            return existentes.stream().map(DoseResponse::from).toList();
        }

        List<Dose> novas = new ArrayList<>();
        for (AgendamentoRequest.Item item : request.itens()) {
            item.horarios().stream().distinct().forEach(horario -> novas.add(Dose.agendar(request.prescricaoId(),
                    item.itemPrescricaoId(), request.internacaoId(), item.medicacaoId(), horario)));
        }
        Map<Long, Integer> reserva = Estoque.porMedicacao(novas);
        estoque.reservar(reserva);
        try {
            return transacao.execute(status -> doseRepository.saveAll(novas).stream()
                    .sorted((a, b) -> a.getHorarioPrevisto().compareTo(b.getHorarioPrevisto()))
                    .map(DoseResponse::from)
                    .toList());
        } catch (RuntimeException falha) {
            estoque.devolver(reserva);
            throw falha;
        }
    }

    /** A prescrição foi desativada: as pendentes viram CANCELADA, as aplicadas ficam como estão. Idempotente. */
    @Transactional
    public List<DoseResponse> cancelarDaPrescricao(Long prescricaoId) {
        return cancelar(doseRepository.findByPrescricaoIdOrderByHorarioPrevistoAsc(prescricaoId));
    }

    /** O animal saiu (internação encerrada): as pendentes de todas as prescrições dele viram CANCELADA. Idempotente. */
    @Transactional
    public List<DoseResponse> cancelarDaInternacao(Long internacaoId) {
        return cancelar(doseRepository.findByInternacaoIdOrderByHorarioPrevistoAsc(internacaoId));
    }

    // A devolução ao estoque vem antes do commit: se o msmedications falhar, o cancelamento também é desfeito e
    // quem chamou recebe o erro, em vez de ficar dose cancelada sem a unidade de volta.
    private List<DoseResponse> cancelar(List<Dose> doses) {
        List<Dose> canceladas = new ArrayList<>();
        for (Dose dose : doses) {
            if (dose.cancelar()) {
                canceladas.add(dose);
            }
        }
        estoque.devolver(Estoque.porMedicacao(canceladas));
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
