package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findByAnimalId(Long animalId);
}
