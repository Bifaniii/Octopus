package com.br.octopus_msusuario.repository;

import com.br.octopus_msusuario.domain.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TutorRepository extends JpaRepository<Tutor, UUID> {
}
