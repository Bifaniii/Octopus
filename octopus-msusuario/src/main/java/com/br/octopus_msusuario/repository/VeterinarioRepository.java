package com.br.octopus_msusuario.repository;

import com.br.octopus_msusuario.domain.Veterinario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VeterinarioRepository extends JpaRepository<Veterinario, UUID> {

    boolean existsByCpfCnpj(String cpfCnpj);

    boolean existsByCrmvAndCrmvUf(String crmv, String crmvUf);
}
