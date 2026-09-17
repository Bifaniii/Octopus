package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.Medicacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medicacao> findByFabricanteIgnoreCase(String fabricante);
    Optional<Medicacao> findBynomeComercialIgnoreCase(String nomeComercial);
}
