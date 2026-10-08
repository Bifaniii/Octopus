package com.br.octopus_msusuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.exception.AnimalNaoEncontradoException;

import java.util.UUID;

public interface AnimalRepository extends JpaRepository<Animal, UUID> {

    // Mãe informada no cadastro do animal: null quando não informada, 404 quando o id não existe.
    default Animal buscarMae(UUID maeId) {
        if (maeId == null) {
            return null;
        }
        return findById(maeId)
                .orElseThrow(() -> new AnimalNaoEncontradoException("Mãe não encontrada com o id: " + maeId));
    }
}
