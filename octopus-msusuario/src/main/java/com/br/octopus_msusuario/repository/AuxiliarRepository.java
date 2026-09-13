package com.br.octopus_msusuario.repository;

import com.br.octopus_msusuario.domain.Auxiliar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuxiliarRepository extends JpaRepository<Auxiliar, UUID> {

    boolean existsByCpf(String cpf);
}
