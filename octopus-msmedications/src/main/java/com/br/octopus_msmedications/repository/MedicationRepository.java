package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.Medicacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationRepository extends JpaRepository<Medicacao, Long> {
    List<Medicacao> findByManufacturerIgnoreCase(String manufacturer);
    Optional<Medicacao> findByCommercialNameIgnoreCase(String commercialName);
}