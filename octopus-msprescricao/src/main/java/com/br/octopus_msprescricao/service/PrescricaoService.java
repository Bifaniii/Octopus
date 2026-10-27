package com.br.octopus_msprescricao.service;

import com.br.octopus_msprescricao.client.DoseClient;
import com.br.octopus_msprescricao.client.InternacaoClient;
import com.br.octopus_msprescricao.client.MedicacaoClient;
import com.br.octopus_msprescricao.client.dto.AgendamentoDosesDto;
import com.br.octopus_msprescricao.client.dto.DoseDto;
import com.br.octopus_msprescricao.client.dto.InternacaoDto;
import com.br.octopus_msprescricao.client.dto.MedicacaoDto;
import com.br.octopus_msprescricao.domain.ItemPrescricao;
import com.br.octopus_msprescricao.domain.Prescricao;
import com.br.octopus_msprescricao.dto.request.PrescricaoRequest;
import com.br.octopus_msprescricao.dto.response.PrescricaoResponse;
import com.br.octopus_msprescricao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msprescricao.exception.RegraDeNegocioException;
import com.br.octopus_msprescricao.repository.PrescricaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
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
    // Mudança contratada: o item "se necessário" passa a existir em 01/11/2026. Configurável para ensaiar antes.
    private final LocalDate seNecessarioAPartirDe;

    public PrescricaoService(PrescricaoRepository prescricaoRepository, InternacaoClient internacaoClient,
                             MedicacaoClient medicacaoClient, DoseClient doseClient, TransactionTemplate transacao,
                             Clock clock,
                             @Value("${app.prescricao.se-necessario-a-partir-de:2026-11-01}") LocalDate seNecessarioAPartirDe) {
        this.prescricaoRepository = prescricaoRepository;
        this.internacaoClient = internacaoClient;
        this.medicacaoClient = medicacaoClient;
        this.doseClient = doseClient;
        this.transacao = transacao;
        this.clock = clock;
        this.seNecessarioAPartirDe = seNecessarioAPartirDe;
    }

    /**
     * As consultas ao ms-internacao e ao msmedications vêm antes e fora da transação. Depois a prescrição é gravada,
     * as doses são agendadas no octopus-msdoses e só então a internação passa a Em tratamento: o ms-internacao
     * recusa essa transição sem prescrição ativa, então ela precisa existir antes. Uma transação local não desfaz o
     * que o outro módulo já gravou (um timeout pode chegar depois de ele ter salvo), então, se o agendamento ou o
     * início do tratamento falhar, a prescrição é desativada e as doses que tenham sido criadas são canceladas: nada
     * fica ativo pela metade.
     */
    public PrescricaoResponse criar(PrescricaoRequest request, String veterinarioEmail) {
        InternacaoDto internacao = internacaoClient.buscar(request.internacaoId());
        if (!STATUS_QUE_ACEITAM_PRESCRICAO.contains(internacao.status())) {
            throw new RegraDeNegocioException("A internação " + internacao.id() + " está " + internacao.status()
                    + " e não aceita nova prescrição.");
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        exigirSeNecessarioEmVigor(request, agora.toLocalDate());
        Prescricao prescricao = request.toEntity(veterinarioEmail, agora);
        Map<Long, MedicacaoDto> medicacoes = new HashMap<>();
        for (ItemPrescricao item : prescricao.getItens()) {
            medicacoes.computeIfAbsent(item.getMedicacaoId(), medicacaoClient::buscar);
        }
        exigirSemInteracaoNoMesmoHorario(prescricao.getItens(), medicacoes, internacao.id(), LocalDateTime.MIN);
        exigirEstoque(prescricao.getItens(), medicacoes);

        Prescricao salva = transacao.execute(status -> prescricaoRepository.save(prescricao));
        try {
            doseClient.agendar(AgendamentoDosesDto.de(salva, medicacoes));
            internacaoClient.iniciarTratamento(internacao.id());
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

    /**
     * Devolve ativas e inativas, com {@code ativo} e {@code situacao}. Com {@code internacaoId}, só as daquela
     * internação. Antes de responder, conclui as prescrições que já acabaram (ver {@link #concluirAsQueAcabaram}).
     */
    public List<PrescricaoResponse> listar(Long internacaoId) {
        List<PrescricaoResponse> lista = transacao.execute(status -> respostas(internacaoId == null
                ? prescricaoRepository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm"))
                : prescricaoRepository.findByInternacaoIdOrderByCriadoEmDesc(internacaoId)));
        if (!concluirAsQueAcabaram(lista)) {
            return lista;
        }
        return transacao.execute(status -> respostas(internacaoId == null
                ? prescricaoRepository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm"))
                : prescricaoRepository.findByInternacaoIdOrderByCriadoEmDesc(internacaoId)));
    }

    public PrescricaoResponse buscar(Long id) {
        PrescricaoResponse resposta = transacao.execute(status -> PrescricaoResponse.from(obter(id)));
        if (!concluirAsQueAcabaram(List.of(resposta))) {
            return resposta;
        }
        return transacao.execute(status -> PrescricaoResponse.from(obter(id)));
    }

    private List<PrescricaoResponse> respostas(List<Prescricao> prescricoes) {
        return prescricoes.stream().map(PrescricaoResponse::from).toList();
    }

    /**
     * Uma prescrição ativa acaba quando o animal já saiu (internação encerrada) ou quando não resta dose pendente
     * nem item "se necessário" ativo no msdoses (todas aplicadas, perdidas ou canceladas). Ela vira inativa e
     * "concluída"; só isso, nada é apagado. Roda ao ler, como a varredura de doses perdidas: se o msdoses ou o
     * ms-internacao não responderem, a prescrição fica como está e a listagem não falha. As criadas há menos de 2
     * minutos ficam de fora, porque as doses ainda podem estar sendo agendadas. Devolve se concluiu alguma.
     */
    private boolean concluirAsQueAcabaram(List<PrescricaoResponse> lista) {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<PrescricaoResponse> ativas = lista.stream().filter(PrescricaoResponse::ativo).toList();
        if (ativas.isEmpty()) {
            return false;
        }
        Set<Long> concluir = new java.util.HashSet<>();
        List<PrescricaoResponse> aindaEmAberto = new java.util.ArrayList<>();
        Map<Long, Boolean> animalSaiu = new HashMap<>();
        for (PrescricaoResponse p : ativas) {
            boolean saiu = animalSaiu.computeIfAbsent(p.internacaoId(), id -> {
                try {
                    return "ENCERRADA".equals(internacaoClient.buscar(id).status());
                } catch (RuntimeException e) {
                    log.warn("Não deu para ver se a internação {} já encerrou: {}", id, e.getMessage());
                    return false;
                }
            });
            if (saiu) {
                concluir.add(p.id());
            } else if (p.criadoEm().isBefore(agora.minusMinutes(2))) {
                aindaEmAberto.add(p);
            }
        }
        if (!aindaEmAberto.isEmpty()) {
            try {
                for (var situacao : doseClient.situacao(aindaEmAberto.stream().map(PrescricaoResponse::id).toList())) {
                    if (situacao.pendentes() == 0 && !situacao.seNecessarioAtivo()) {
                        concluir.add(situacao.prescricaoId());
                    }
                }
            } catch (RuntimeException e) {
                log.warn("Não deu para ver a situação das doses: {}", e.getMessage());
            }
        }
        if (concluir.isEmpty()) {
            return false;
        }
        transacao.executeWithoutResult(status -> concluir.forEach(id -> {
            Prescricao prescricao = obter(id);
            if (prescricao.isAtivo()) {
                prescricao.concluir(agora);
            }
        }));
        return true;
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
     * Reativa uma prescrição desativada. Só vale com a internação ainda aberta. O RN-06 é conferido de novo contra as
     * prescrições ativas, mas só nos horários que ainda não passaram. O octopus-msdoses traz de volta as doses
     * canceladas que ainda estão na janela e reserva o estoque (422 se faltar saldo). Se nenhuma dose volta, nada
     * é reativado: já não há o que aplicar, e o caminho é uma prescrição nova.
     */
    public PrescricaoResponse ativar(Long id) {
        Prescricao prescricao = transacao.execute(status -> {
            Prescricao p = obter(id);
            p.getItens().size(); // carrega os itens: são usados fora desta transação
            return p;
        });
        if (prescricao.isAtivo()) {
            return PrescricaoResponse.from(prescricao);
        }
        if (prescricao.getConcluidaEm() != null) {
            throw new RegraDeNegocioException("Esta prescrição já foi concluída: não há mais doses a aplicar. "
                    + "Faça uma nova prescrição.");
        }

        InternacaoDto internacao = internacaoClient.buscar(prescricao.getInternacaoId());
        if (!STATUS_QUE_ACEITAM_PRESCRICAO.contains(internacao.status())) {
            throw new RegraDeNegocioException("A internação " + internacao.id() + " está " + internacao.status()
                    + " e não aceita reativar a prescrição.");
        }
        Map<Long, MedicacaoDto> medicacoes = new HashMap<>();
        for (ItemPrescricao item : prescricao.getItens()) {
            medicacoes.computeIfAbsent(item.getMedicacaoId(), medicacaoClient::buscar);
        }
        // O que já passou há mais de 30 minutos não volta, então também não conta para o RN-06.
        exigirSemInteracaoNoMesmoHorario(prescricao.getItens(), medicacoes, internacao.id(),
                LocalDateTime.now(clock).minusMinutes(30));

        List<DoseDto> doses = doseClient.reativar(id);
        boolean temSeNecessario = prescricao.getItens().stream().anyMatch(ItemPrescricao::isSeNecessario);
        if (!temSeNecessario && doses.stream().noneMatch(d -> "PENDENTE".equals(d.status()))) {
            throw new RegraDeNegocioException("Todas as doses desta prescrição já passaram. Faça uma nova prescrição.");
        }
        try {
            return transacao.execute(status -> {
                Prescricao p = obter(id);
                p.setAtivo(true);
                return PrescricaoResponse.from(p);
            });
        } catch (RuntimeException falha) {
            // As doses já voltaram no msdoses: se a gravação aqui falhar, elas são canceladas de novo.
            try {
                doseClient.cancelar(id);
            } catch (RuntimeException e) {
                log.warn("Prescrição {} segue inativa, mas o cancelamento das doses reativadas falhou: {}", id, e.getMessage());
            }
            throw falha;
        }
    }

    /**
     * RN-06: a prescrição não pode ter dois medicamentos com interação proibida no mesmo horário. Vale entre os
     * itens novos e entre eles e os itens das prescrições ativas da mesma internação, aplicados no mesmo animal.
     * As interações vêm do octopus-msmedications, que grava o par nos dois sentidos: basta a lista do medicamento
     * novo.
     */
    private void exigirSemInteracaoNoMesmoHorario(List<ItemPrescricao> novos, Map<Long, MedicacaoDto> medicacoes,
                                                  Long internacaoId, LocalDateTime aPartirDe) {
        for (int i = 0; i < novos.size(); i++) {
            for (int j = i + 1; j < novos.size(); j++) {
                conferir(novos.get(i), novos.get(j), medicacoes, "", aPartirDe);
            }
        }

        List<ItemPrescricao> emUso = prescricaoRepository.itensEmUso(internacaoId);
        for (ItemPrescricao novo : novos) {
            for (ItemPrescricao existente : emUso) {
                conferir(novo, existente, medicacoes, ", já prescrito nesta internação", aPartirDe);
            }
        }
    }

    private void exigirSeNecessarioEmVigor(PrescricaoRequest request, LocalDate hoje) {
        boolean temSeNecessario = request.itens().stream().anyMatch(item -> item.ehSeNecessario());
        if (temSeNecessario && hoje.isBefore(seNecessarioAPartirDe)) {
            throw new RegraDeNegocioException("A prescrição \"se necessário\" só vale a partir de "
                    + seNecessarioAPartirDe.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
        }
    }

    /**
     * Cada dose agendada separa 1 unidade do estoque (a reserva de fato é feita pelo octopus-msdoses ao agendar).
     * Conferir aqui evita gravar uma prescrição que seria desfeita em seguida; duas prescrições ao mesmo tempo
     * ainda podem disputar o último saldo, e aí a recusa vem do agendamento. O item "se necessário" reserva na hora
     * de cada aplicação; aqui só se exige que haja ao menos uma unidade.
     */
    private void exigirEstoque(List<ItemPrescricao> itens, Map<Long, MedicacaoDto> medicacoes) {
        Map<Long, Integer> pedidas = new HashMap<>();
        for (ItemPrescricao item : itens) {
            pedidas.merge(item.getMedicacaoId(), item.isSeNecessario() ? 1 : item.horarios().size(), Integer::sum);
        }
        pedidas.forEach((medicacaoId, quantidade) -> {
            MedicacaoDto medicacao = medicacoes.get(medicacaoId);
            if (medicacao.quantidade() < quantidade) {
                throw new RegraDeNegocioException("Estoque insuficiente de " + medicacao.nomeComercial() + ": "
                        + medicacao.quantidade() + " em estoque, " + quantidade + " doses prescritas.");
            }
        });
    }

    private void conferir(ItemPrescricao novo, ItemPrescricao outro, Map<Long, MedicacaoDto> medicacoes,
                          String complemento, LocalDateTime aPartirDe) {
        MedicacaoDto medicacao = medicacoes.get(novo.getMedicacaoId());
        String nomeOutro = medicacao.nomeDaInteracaoCom(outro.getMedicacaoId());
        if (nomeOutro == null) {
            return;
        }
        primeiroHorarioEmComum(novo, outro, aPartirDe).ifPresent(horario -> {
            throw new RegraDeNegocioException("RN-06: " + medicacao.nomeComercial() + " tem interação proibida com "
                    + nomeOutro + complemento + ", e os dois cairiam no mesmo horário (" + horario.format(HORARIO)
                    + ").");
        });
    }

    // Só os horários a partir de `aPartirDe`: na reativação o que já passou não pode barrar a prescrição.
    private Optional<LocalDateTime> primeiroHorarioEmComum(ItemPrescricao a, ItemPrescricao b, LocalDateTime aPartirDe) {
        Set<LocalDateTime> horariosDeB = Set.copyOf(b.horarios());
        return a.horarios().stream()
                .filter(h -> !h.isBefore(aPartirDe))
                .filter(horariosDeB::contains)
                .findFirst();
    }

    private Prescricao obter(Long id) {
        return prescricaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prescrição não encontrada com o id: " + id));
    }
}
