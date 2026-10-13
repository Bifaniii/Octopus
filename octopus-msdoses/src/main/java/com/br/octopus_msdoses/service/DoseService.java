package com.br.octopus_msdoses.service;

import com.br.octopus_msdoses.domain.Dose;
import com.br.octopus_msdoses.domain.ItemSeNecessario;
import com.br.octopus_msdoses.domain.enums.StatusDose;
import com.br.octopus_msdoses.dto.request.AgendamentoRequest;
import com.br.octopus_msdoses.dto.response.DoseResponse;
import com.br.octopus_msdoses.dto.response.ItemSeNecessarioResponse;
import com.br.octopus_msdoses.dto.response.PainelResponse;
import com.br.octopus_msdoses.exception.RecursoNaoEncontradoException;
import com.br.octopus_msdoses.exception.RegraDeNegocioException;
import com.br.octopus_msdoses.repository.DoseRepository;
import com.br.octopus_msdoses.repository.ItemSeNecessarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Os métodos não são @Transactional: a Varredura (RN-04, RN-07) roda antes, numa transação própria, e as chamadas
// ao msmedications ficam fora das transações deste serviço sempre que dá.
@Service
@RequiredArgsConstructor
public class DoseService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final DoseRepository doseRepository;
    private final ItemSeNecessarioRepository itemSeNecessarioRepository;
    private final Estoque estoque;
    private final Varredura varredura;
    private final TransactionTemplate transacao;
    private final Clock clock;

    /**
     * Uma dose por horário de cada item com horário fixo; o item "se necessário" só é registrado, sem dose. Idempotente:
     * se a prescrição já foi agendada, devolve o que existe, para que uma nova tentativa do octopus-msprescricao
     * (depois de um timeout, por exemplo) não duplique a agenda. Cada dose nova separa 1 unidade do estoque do
     * medicamento. A reserva vem antes de gravar, fora da transação; se a gravação falhar, o que foi reservado volta.
     */
    public List<DoseResponse> agendar(AgendamentoRequest request) {
        request.itens().forEach(DoseService::validar);
        List<Dose> existentes = transacao.execute(status -> {
            boolean jaAgendada = !itemSeNecessarioRepository.findByPrescricaoId(request.prescricaoId()).isEmpty();
            List<Dose> doses = doseRepository.findByPrescricaoIdOrderByHorarioPrevistoAsc(request.prescricaoId());
            return jaAgendada || !doses.isEmpty() ? doses : null;
        });
        if (existentes != null) {
            return existentes.stream().map(DoseResponse::from).toList();
        }

        List<Dose> novas = new ArrayList<>();
        List<ItemSeNecessario> seNecessario = new ArrayList<>();
        for (AgendamentoRequest.Item item : request.itens()) {
            if (item.ehSeNecessario()) {
                seNecessario.add(ItemSeNecessario.registrar(item.itemPrescricaoId(), request.prescricaoId(),
                        request.internacaoId(), item.medicacaoId(), item.esquema(), item.intervaloMinimoHoras()));
            } else {
                item.horarios().stream().distinct().forEach(horario -> novas.add(Dose.agendar(request.prescricaoId(),
                        item.itemPrescricaoId(), request.internacaoId(), item.medicacaoId(), item.esquema(), horario)));
            }
        }
        Map<Long, Integer> reserva = Estoque.porMedicacao(novas);
        estoque.reservar(reserva);
        try {
            return transacao.execute(status -> {
                itemSeNecessarioRepository.saveAll(seNecessario);
                return doseRepository.saveAll(novas).stream()
                        .sorted(Comparator.comparing(Dose::getHorarioPrevisto))
                        .map(DoseResponse::from)
                        .toList();
            });
        } catch (RuntimeException falha) {
            estoque.devolver(reserva);
            throw falha;
        }
    }

    private static void validar(AgendamentoRequest.Item item) {
        boolean temHorarios = item.horarios() != null && !item.horarios().isEmpty();
        if (item.ehSeNecessario() && (item.intervaloMinimoHoras() == null || temHorarios)) {
            throw new RegraDeNegocioException("O item " + item.itemPrescricaoId()
                    + " é \"se necessário\": precisa do intervalo mínimo e não tem horários.");
        }
        if (!item.ehSeNecessario() && !temHorarios) {
            throw new RegraDeNegocioException("O item " + item.itemPrescricaoId() + " precisa de ao menos um horário.");
        }
    }

    /** A prescrição foi desativada: as pendentes viram CANCELADA, as aplicadas ficam como estão. Idempotente. */
    public List<DoseResponse> cancelarDaPrescricao(Long prescricaoId) {
        return transacao.execute(status -> {
            itemSeNecessarioRepository.findByPrescricaoId(prescricaoId).forEach(ItemSeNecessario::desativar);
            return cancelar(doseRepository.findByPrescricaoIdOrderByHorarioPrevistoAsc(prescricaoId));
        });
    }

    /** O animal saiu (internação encerrada): as pendentes de todas as prescrições dele viram CANCELADA. Idempotente. */
    public List<DoseResponse> cancelarDaInternacao(Long internacaoId) {
        return transacao.execute(status -> {
            itemSeNecessarioRepository.findByInternacaoId(internacaoId).forEach(ItemSeNecessario::desativar);
            return cancelar(doseRepository.findByInternacaoIdOrderByHorarioPrevistoAsc(internacaoId));
        });
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
    public List<DoseResponse> listar(Long internacaoId, Long prescricaoId, StatusDose status, LocalDate de,
                                     LocalDate ate) {
        varredura.executar(agora());
        LocalDateTime inicio = de == null ? LocalDateTime.of(1900, 1, 1, 0, 0) : de.atStartOfDay();
        LocalDateTime fim = ate == null ? LocalDateTime.of(9999, 1, 1, 0, 0) : ate.plusDays(1).atStartOfDay();
        return transacao.execute(s -> doseRepository.listar(internacaoId, prescricaoId, status, inicio, fim).stream()
                .map(DoseResponse::from)
                .toList());
    }

    public DoseResponse buscar(Long id) {
        varredura.executar(agora());
        return transacao.execute(s -> DoseResponse.from(obter(id)));
    }

    /** Tela 6: atrasadas, próximas (vencem na próxima hora), perdidas nas últimas 24 h e itens "se necessário". */
    public PainelResponse painel(Long internacaoId) {
        LocalDateTime agora = agora();
        varredura.executar(agora);
        return transacao.execute(s -> new PainelResponse(
                agora,
                doses(StatusDose.PENDENTE, agora.minus(Dose.JANELA), agora.minusNanos(1_000), internacaoId),
                doses(StatusDose.PENDENTE, agora, agora.plusHours(1), internacaoId),
                doses(StatusDose.PERDIDA, agora.minusHours(24), agora, internacaoId),
                itemSeNecessarioRepository.ativos(internacaoId).stream()
                        .map(item -> seNecessario(item, agora))
                        .toList()));
    }

    private List<DoseResponse> doses(StatusDose status, LocalDateTime de, LocalDateTime ate, Long internacaoId) {
        return doseRepository.doPainel(status, de, ate, internacaoId).stream().map(DoseResponse::from).toList();
    }

    private ItemSeNecessarioResponse seNecessario(ItemSeNecessario item, LocalDateTime agora) {
        return doseRepository.findFirstByItemPrescricaoIdAndStatusOrderByAplicadaEmDesc(item.getItemPrescricaoId(),
                        StatusDose.APLICADA)
                .map(d -> ItemSeNecessarioResponse.from(item, d.getAplicadaEm(), d.getAplicadaPor(), agora))
                .orElseGet(() -> ItemSeNecessarioResponse.from(item, null, null, agora));
    }

    /** RN-03 e RN-05: grava o auxiliar (dono do token) e a hora da clínica, dentro da janela. Não há edição depois. */
    public DoseResponse aplicar(Long id, String auxiliarEmail, String observacao) {
        LocalDateTime agora = agora();
        varredura.executar(agora);
        return transacao.execute(s -> {
            Dose dose = obter(id);
            dose.aplicar(auxiliarEmail, agora, observacao);
            return DoseResponse.from(doseRepository.saveAndFlush(dose));
        });
    }

    /**
     * Mudança contratada de 01/11: aplicação de um item "se necessário", recusada antes do intervalo mínimo desde a
     * última. A unidade do estoque é reservada na hora da aplicação, já que o item não tem doses agendadas. A regra
     * é conferida duas vezes: antes de reservar, para recusar rápido, e de novo com o item travado.
     */
    public DoseResponse aplicarSeNecessario(Long itemPrescricaoId, String auxiliarEmail, String observacao) {
        LocalDateTime agora = agora();
        ItemSeNecessario item = transacao.execute(s -> {
            ItemSeNecessario encontrado = itemSeNecessarioRepository.findByItemPrescricaoId(itemPrescricaoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Item \"se necessário\" não encontrado: " + itemPrescricaoId));
            conferirIntervalo(encontrado, agora);
            return encontrado;
        });
        Map<Long, Integer> reserva = Map.of(item.getMedicacaoId(), 1);
        estoque.reservar(reserva);
        try {
            return transacao.execute(s -> {
                ItemSeNecessario travado = itemSeNecessarioRepository.travarPorItem(itemPrescricaoId).orElseThrow();
                conferirIntervalo(travado, agora);
                return DoseResponse.from(doseRepository.save(
                        Dose.aplicadaSeNecessario(travado, auxiliarEmail, agora, observacao)));
            });
        } catch (RuntimeException falha) {
            estoque.devolver(reserva);
            throw falha;
        }
    }

    private void conferirIntervalo(ItemSeNecessario item, LocalDateTime agora) {
        if (!item.isAtivo()) {
            throw new RegraDeNegocioException("A prescrição deste item foi encerrada.");
        }
        doseRepository.findFirstByItemPrescricaoIdAndStatusOrderByAplicadaEmDesc(item.getItemPrescricaoId(),
                StatusDose.APLICADA).ifPresent(ultima -> {
            LocalDateTime liberado = ultima.getAplicadaEm().plusHours(item.getIntervaloMinimoHoras());
            if (agora.isBefore(liberado)) {
                throw new RegraDeNegocioException("Intervalo mínimo de " + item.getIntervaloMinimoHoras()
                        + " h: a última aplicação foi às " + ultima.getAplicadaEm().format(HORA)
                        + ", a próxima só a partir de " + liberado.format(HORA) + ".");
            }
        });
    }

    private Dose obter(Long id) {
        return doseRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dose não encontrada com o id: " + id));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
