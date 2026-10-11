package com.br.octopus_msdoses.repository;

import com.br.octopus_msdoses.domain.Dose;
import com.br.octopus_msdoses.domain.enums.StatusDose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface DoseRepository extends JpaRepository<Dose, Long> {

    List<Dose> findByPrescricaoIdOrderByHorarioPrevistoAsc(Long prescricaoId);

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
}
