package com.br.octopus_msprescricao.service;

import com.br.octopus_msprescricao.client.DoseClient;
import com.br.octopus_msprescricao.client.InternacaoClient;
import com.br.octopus_msprescricao.client.MedicacaoClient;
import com.br.octopus_msprescricao.client.dto.AgendamentoDosesDto;
import com.br.octopus_msprescricao.client.dto.InternacaoDto;
import com.br.octopus_msprescricao.client.dto.MedicacaoDto;
import com.br.octopus_msprescricao.domain.ItemPrescricao;
import com.br.octopus_msprescricao.domain.Prescricao;
import com.br.octopus_msprescricao.dto.request.PrescricaoRequest;
import com.br.octopus_msprescricao.dto.response.PrescricaoResponse;
import com.br.octopus_msprescricao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msprescricao.exception.RegraDeNegocioException;
import com.br.octopus_msprescricao.repository.PrescricaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescricaoService {

    // Depois da alta (autorizada ou a pedido do tutor) e com a internação encerrada não se prescreve mais.
    private static final Set<String> STATUS_QUE_ACEITAM_PRESCRICAO = Set.of("ADMITIDA", "EM_TRATAMENTO", "ISOLAMENTO");
    private static final DateTimeFormatter HORARIO = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final PrescricaoRepository prescricaoRepository;
    private final InternacaoClient internacaoClient;
    private final MedicacaoClient medicacaoClient;
    private final DoseClient doseClient;
    private final TransactionTemplate transacao;
    private final Clock clock;

    /**
     * As consultas ao ms-internacao e ao msmedications vêm antes e fora da transação. O iniciar-tratamento é
     * idempotente, então pode vir antes de gravar: se ele falhar, nada é gravado. Depois a prescrição é gravada e
     * só então as doses são agendadas no octopus-msdoses. Uma transação local não desfaz o que o outro módulo já
     * gravou (um timeout pode chegar depois de ele ter salvo), então, se o agendamento falhar, a prescrição é
     * desativada e as doses que tenham sido criadas são canceladas: nada fica ativo pela metade.
     */
    public PrescricaoResponse criar(PrescricaoRequest request, String veterinarioEmail) {
        InternacaoDto internacao = internacaoClient.buscar(request.internacaoId());
        if (!STATUS_QUE_ACEITAM_PRESCRICAO.contains(internacao.status())) {
            throw new RegraDeNegocioException("A internação " + internacao.id() + " está " + internacao.status()
                    + " e não aceita nova prescrição.");
        }

        Prescricao prescricao = request.toEntity(veterinarioEmail, LocalDateTime.now(clock));
        exigirSemInteracaoNoMesmoHorario(prescricao.getItens(), internacao.id());

        internacaoClient.iniciarTratamento(internacao.id());

        Prescricao salva = transacao.execute(status -> prescricaoRepository.save(prescricao));
        try {
            doseClient.agendar(AgendamentoDosesDto.de(salva));
        } catch (RuntimeException falha) {
            desfazerAgendamento(salva.getId());
            throw falha;
        }
        return PrescricaoResponse.from(salva);
    }

    private void desfazerAgendamento(Long prescricaoId) {
        transacao.executeWithoutResult(status -> obter(prescricaoId).setAtivo(false));
        try {
            doseClient.cancelar(prescricaoId);
        } catch (RuntimeException e) {
            log.warn("Prescrição {} desativada, mas o cancelamento das doses falhou: {}", prescricaoId, e.getMessage());
        }
    }

    /** Devolve ativas e inativas, com o campo {@code ativo}. Com {@code internacaoId}, só as daquela internação. */
    @Transactional(readOnly = true)
    public List<PrescricaoResponse> listar(Long internacaoId) {
        List<Prescricao> prescricoes = internacaoId == null
                ? prescricaoRepository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm"))
                : prescricaoRepository.findByInternacaoIdOrderByCriadoEmDesc(internacaoId);
        return prescricoes.stream().map(PrescricaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PrescricaoResponse buscar(Long id) {
        return PrescricaoResponse.from(obter(id));
    }

    /** Nada é apagado: a prescrição fica com ativo = false e as doses ainda pendentes são canceladas. */
    public PrescricaoResponse desativar(Long id) {
        return transacao.execute(status -> {
            Prescricao prescricao = obter(id);
            if (prescricao.isAtivo()) {
                prescricao.setAtivo(false);
                doseClient.cancelar(id);
            }
            return PrescricaoResponse.from(prescricao);
        });
    }

    /**
     * RN-06: a prescrição não pode ter dois medicamentos com interação proibida no mesmo horário. Vale entre os
     * itens novos e entre eles e os itens das prescrições ativas da mesma internação, aplicados no mesmo animal.
     * As interações vêm do octopus-msmedications, que grava o par nos dois sentidos: basta a lista do medicamento
     * novo.
     */
    private void exigirSemInteracaoNoMesmoHorario(List<ItemPrescricao> novos, Long internacaoId) {
        Map<Long, MedicacaoDto> medicacoes = new HashMap<>();
        for (ItemPrescricao item : novos) {
            medicacoes.computeIfAbsent(item.getMedicacaoId(), medicacaoClient::buscar);
        }

        for (int i = 0; i < novos.size(); i++) {
            for (int j = i + 1; j < novos.size(); j++) {
                conferir(novos.get(i), novos.get(j), medicacoes, "");
            }
        }

        List<ItemPrescricao> emUso = prescricaoRepository.itensEmUso(internacaoId);
        for (ItemPrescricao novo : novos) {
            for (ItemPrescricao existente : emUso) {
                conferir(novo, existente, medicacoes, ", já prescrito nesta internação");
            }
        }
    }

    private void conferir(ItemPrescricao novo, ItemPrescricao outro, Map<Long, MedicacaoDto> medicacoes,
                          String complemento) {
        MedicacaoDto medicacao = medicacoes.get(novo.getMedicacaoId());
        String nomeOutro = medicacao.nomeDaInteracaoCom(outro.getMedicacaoId());
        if (nomeOutro == null) {
            return;
        }
        primeiroHorarioEmComum(novo, outro).ifPresent(horario -> {
            throw new RegraDeNegocioException("RN-06: " + medicacao.nomeComercial() + " tem interação proibida com "
                    + nomeOutro + complemento + ", e os dois cairiam no mesmo horário (" + horario.format(HORARIO)
                    + ").");
        });
    }

    private Optional<LocalDateTime> primeiroHorarioEmComum(ItemPrescricao a, ItemPrescricao b) {
        Set<LocalDateTime> horariosDeB = Set.copyOf(b.horarios());
        return a.horarios().stream().filter(horariosDeB::contains).findFirst();
    }

    private Prescricao obter(Long id) {
        return prescricaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prescrição não encontrada com o id: " + id));
    }
}
