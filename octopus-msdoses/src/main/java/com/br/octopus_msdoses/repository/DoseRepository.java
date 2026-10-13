package com.br.octopus_msdoses.repository;

import com.br.octopus_msdoses.domain.Dose;
import com.br.octopus_msdoses.domain.enums.StatusDose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DoseRepository extends JpaRepository<Dose, Long> {

    List<Dose> findByPrescricaoIdOrderByHorarioPrevistoAsc(Long prescricaoId);

    List<Dose> findByInternacaoIdOrderByHorarioPrevistoAsc(Long internacaoId);

    // Filtros opcionais: o que vier nulo não restringe. O período compara o horário previsto.
    @Query("""
            select d from Dose d
            where (:internacaoId is null or d.internacaoId = :internacaoId)
              and (:prescricaoId is null or d.prescricaoId = :prescricaoId)
              and (:status is null or d.status = :status)
              and d.horarioPrevisto >= :de and d.horarioPrevisto < :ate
            order by d.horarioPrevisto asc, d.id asc
            """)
    List<Dose> listar(Long internacaoId, Long prescricaoId, StatusDose status, LocalDateTime de, LocalDateTime ate);

    /** RN-04: pendentes cuja janela fechou antes de {@code limite} (horário previsto anterior a limite - 30 min). */
    List<Dose> findByStatusAndHorarioPrevistoBeforeOrderByHorarioPrevistoAscIdAsc(StatusDose status,
                                                                                  LocalDateTime limite);

    /** RN-07: as pendentes do mesmo item depois da perdida, da última para a primeira (ver Varredura). */
    List<Dose> findByItemPrescricaoIdAndStatusAndHorarioPrevistoAfterOrderByHorarioPrevistoDesc(
            Long itemPrescricaoId, StatusDose status, LocalDateTime horario);

    /** Painel: doses de um status com horário no intervalo [de, ate]. */
    @Query("""
            select d from Dose d
            where d.status = :status and d.horarioPrevisto >= :de and d.horarioPrevisto <= :ate
              and (:internacaoId is null or d.internacaoId = :internacaoId)
            order by d.horarioPrevisto asc, d.id asc
            """)
    List<Dose> doPainel(StatusDose status, LocalDateTime de, LocalDateTime ate, Long internacaoId);

    /** Última aplicação de um item "se necessário", para o intervalo mínimo. */
    Optional<Dose> findFirstByItemPrescricaoIdAndStatusOrderByAplicadaEmDesc(Long itemPrescricaoId,
                                                                             StatusDose status);
}
