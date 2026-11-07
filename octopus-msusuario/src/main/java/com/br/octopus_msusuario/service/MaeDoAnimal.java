package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.enums.Especie;
import com.br.octopus_msusuario.domain.enums.SexoAnimal;
import com.br.octopus_msusuario.exception.RegraNegocioException;

/**
 * Regras da mãe informada no cadastro e na edição do animal, usadas pelo AnimalService e pelo TutorService. A mãe é
 * o que o ms-internacao confere na RN-01 (baia de ninhada só com filhotes da mesma mãe), então precisa ser plausível:
 * uma fêmea, da mesma espécie, e fora da linha de ascendência do próprio animal.
 */
final class MaeDoAnimal {

    private MaeDoAnimal() {
    }

    /** `animalId` é nulo no cadastro, quando o animal ainda não existe e não pode estar na própria ascendência. */
    static void exigirValida(Long animalId, Especie especie, Animal mae) {
        if (mae == null) {
            return;
        }
        if (animalId != null) {
            for (Animal atual = mae; atual != null; atual = atual.getMae()) {
                if (atual.getId().equals(animalId)) {
                    throw new RegraNegocioException("Um animal não pode ser mãe de si mesmo nem da própria ascendência.");
                }
            }
        }
        if (mae.getSexo() == null) {
            throw new RegraNegocioException("O sexo de " + mae.getNome() + " não foi informado. Edite o cadastro dela "
                    + "antes de indicá-la como mãe.");
        }
        if (mae.getSexo() != SexoAnimal.FEMEA) {
            throw new RegraNegocioException(mae.getNome() + " é macho e não pode ser indicado como mãe.");
        }
        if (mae.getEspecie() != especie) {
            throw new RegraNegocioException("A mãe precisa ser da mesma espécie do animal: " + mae.getNome() + " é "
                    + (mae.getEspecie() == null ? "de espécie não informada" : mae.getEspecie()) + ".");
        }
    }
}
