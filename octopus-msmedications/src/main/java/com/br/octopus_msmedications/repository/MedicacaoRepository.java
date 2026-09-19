package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.Medicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface MedicacaoRepository extends JpaRepository<Medicacao, Long> {
    List<Medicacao> findByFabricanteIgnoreCase(String fabricante);
    Optional<Medicacao> findByNomeComercialIgnoreCase(String nomeComercial);
}
