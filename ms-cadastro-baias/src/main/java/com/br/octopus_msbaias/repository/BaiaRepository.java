package com.br.octopus_msbaias.repository;

import com.br.octopus_msbaias.domain.Baia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BaiaRepository extends JpaRepository<Baia, UUID> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    long countByAtivoTrue();
}
