package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.Medicacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicacaoRepository extends JpaRepository<Medicacao, Long> {

    List<Medicacao> findByFabricanteIgnoreCase(String fabricante);

    List<Medicacao> findByNomeComercialContainingIgnoreCase(String nomeComercial);

    Optional<Medicacao> findByNumeroRegistroAnvisa(String numeroRegistroAnvisa);

    boolean existsByNumeroRegistroAnvisa(String numeroRegistroAnvisa);
}
