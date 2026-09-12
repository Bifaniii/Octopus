package com.br.octopus_msusuario.repository;

import com.br.octopus_msusuario.domain.Recepcionista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecepcionistaRepository extends JpaRepository<Recepcionista, UUID> {

    boolean existsByCpf(String cpf);
}
