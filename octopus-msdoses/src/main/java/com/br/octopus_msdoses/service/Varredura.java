package com.br.octopus_msdoses.service;

import com.br.octopus_msdoses.domain.Dose;
import com.br.octopus_msdoses.domain.enums.StatusDose;
import com.br.octopus_msdoses.domain.enums.TipoEsquema;
import com.br.octopus_msdoses.repository.DoseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * RN-04 e RN-07. Roda antes de toda consulta e aplicação, então quem abre o painel ou o relatório já vê a dose
 * perdida, sem job agendado. Um job não teria o token de ninguém para devolver o estoque ao msmedications.
 *
 * <p>Dose pendente cuja janela fechou vira PERDIDA. Esquema contínuo: é reaplicada no horário em que a perda foi
 * constatada, e as pendentes seguintes do mesmo item andam o mesmo tanto, mantendo o intervalo. A reaplicação usa a
 * unidade já reservada da perdida. Esquema sintomático: é pulada, as seguintes ficam como estão e a unidade volta ao
 * estoque, já que não chegou a ser preparada.
 */
@Slf4j
@Component
class Varredura {

    private final DoseRepository doseRepository;
    private final Estoque estoque;
    private final TransactionTemplate transacao;

    Varredura(DoseRepository doseRepository, Estoque estoque, PlatformTransactionManager transactionManager) {
        this.doseRepository = doseRepository;
        this.estoque = estoque;
        this.transacao = new TransactionTemplate(transactionManager);
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    void executar(LocalDateTime agora) {
        try {
            transacao.executeWithoutResult(status -> processar(agora));
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException
                 | CannotAcquireLockException e) {
            // Outra requisição varreu as mesmas doses ao mesmo tempo e gravou primeiro: o resultado é o mesmo.
            log.info("Varredura concorrente ignorada: {}", e.getMessage());
        }
    }

    private void processar(LocalDateTime agora) {
        List<Dose> vencidas = doseRepository.findByStatusAndHorarioPrevistoBeforeOrderByHorarioPrevistoAscIdAsc(
                StatusDose.PENDENTE, agora.minus(Dose.JANELA));
        Map<Long, Integer> devolver = new TreeMap<>();
        for (Dose dose : vencidas) {
            // Uma reaplicação anterior nesta mesma varredura pode ter deslocado a dose para depois de agora.
            if (!dose.passouDaJanela(agora)) {
                continue;
            }
            dose.marcarPerdida();
            if (dose.getEsquema() == TipoEsquema.CONTINUO) {
                reaplicar(dose, agora.truncatedTo(ChronoUnit.MINUTES));
            } else {
                devolver.merge(dose.getMedicacaoId(), 1, Integer::sum);
            }
        }
        estoque.devolver(devolver);
    }

    private void reaplicar(Dose perdida, LocalDateTime horario) {
        Duration atraso = Duration.between(perdida.getHorarioPrevisto(), horario);
        // Da última para a primeira, gravando uma de cada vez: deslocar a primeira antes poderia colidir com o
        // horário ainda não deslocado da seguinte no UNIQUE (item, horário).
        for (Dose seguinte : doseRepository.findByItemPrescricaoIdAndStatusAndHorarioPrevistoAfterOrderByHorarioPrevistoDesc(
                perdida.getItemPrescricaoId(), StatusDose.PENDENTE, perdida.getHorarioPrevisto())) {
            seguinte.deslocar(atraso);
            doseRepository.saveAndFlush(seguinte);
        }
        doseRepository.save(Dose.reaplicar(perdida, horario));
    }
}
