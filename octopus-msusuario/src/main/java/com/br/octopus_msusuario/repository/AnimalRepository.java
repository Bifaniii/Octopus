package com.br.octopus_msusuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.octopus_msusuario.domain.Animal;

import java.util.UUID;

public interface AnimalRepository extends JpaRepository<Animal, UUID> {
}
