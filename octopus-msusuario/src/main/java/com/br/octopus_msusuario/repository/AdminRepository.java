package com.br.octopus_msusuario.repository;

import com.br.octopus_msusuario.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
}
