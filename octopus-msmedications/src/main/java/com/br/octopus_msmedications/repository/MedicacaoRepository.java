package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.Medicacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicacaoRepository extends JpaRepository<Medicacao, UUID> {

    List<Medicacao> findByFabricanteIgnoreCase(String fabricante);

    List<Medicacao> findByNomeComercialContainingIgnoreCase(String nomeComercial);

    Optional<Medicacao> findByNumeroRegistroAnvisa(String numeroRegistroAnvisa);

    boolean existsByNumeroRegistroAnvisa(String numeroRegistroAnvisa);
}
