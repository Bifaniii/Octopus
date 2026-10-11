package com.br.octopus_msbaias.repository;

import com.br.octopus_msbaias.domain.Baia;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BaiaRepository extends JpaRepository<Baia, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    long countByAtivoTrue();
}
