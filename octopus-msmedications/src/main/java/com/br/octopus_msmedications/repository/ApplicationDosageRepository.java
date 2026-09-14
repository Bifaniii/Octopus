package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.ApplicationDosage;
import com.br.octopus_msmedications.domain.enums.StatusDosage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationDosageRepository extends JpaRepository<ApplicationDosage, Long> {
    List<ApplicationDosage> findByMedicationId(Long medicationId);
    List<ApplicationDosage> findByStatusAndScheduledTimeBefore(StatusDosage status, LocalDateTime moment);
}
