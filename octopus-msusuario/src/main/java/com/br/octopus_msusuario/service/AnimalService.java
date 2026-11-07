package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.Tutor;
import com.br.octopus_msusuario.domain.enums.SexoAnimal;
import com.br.octopus_msusuario.dto.request.AnimalRequest;
import com.br.octopus_msusuario.dto.response.AnimalResponse;
import com.br.octopus_msusuario.exception.AnimalNaoEncontradoException;
import com.br.octopus_msusuario.exception.RegraNegocioException;
import com.br.octopus_msusuario.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final TutorService tutorService;

    @Transactional
    public AnimalResponse criar(Long tutorId, AnimalRequest request) {
        Tutor tutor = tutorService.obter(tutorId);
        Animal mae = animalRepository.buscarMae(request.maeId());
        MaeDoAnimal.exigirValida(null, request.especie(), mae);
        Animal animal = request.toEntity(mae);
        tutor.adicionarAnimal(animal);
        return AnimalResponse.from(animalRepository.save(animal));
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> listar() {
        return animalRepository.findAll().stream().map(AnimalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AnimalResponse listarPorId(Long id) {
        return AnimalResponse.from(obter(id));
    }

    @Transactional
    public AnimalResponse atualizar(Long id, AnimalRequest request) {
        Animal animal = obter(id);
        Animal mae = animalRepository.buscarMae(request.maeId());
        MaeDoAnimal.exigirValida(animal.getId(), request.especie(), mae);
        exigirCompativelComOsFilhotes(animal, request);

        animal.setNome(request.nome());
        animal.setEspecie(request.especie());
        animal.setSexo(request.sexo());
        animal.setDataUltimaAntirrabica(request.dataAntirrabica());
        animal.setMae(mae);
        return AnimalResponse.from(animal);
    }

    // Quem já é mãe de alguém continua fêmea e da mesma espécie, senão os filhotes ficariam com uma mãe impossível.
    private void exigirCompativelComOsFilhotes(Animal animal, AnimalRequest request) {
        if (!animalRepository.existsByMaeId(animal.getId())) {
            return;
        }
        if (request.sexo() != SexoAnimal.FEMEA) {
            throw new RegraNegocioException(animal.getNome() + " é mãe de animais cadastrados e precisa continuar "
                    + "como fêmea.");
        }
        if (animal.getEspecie() != null && request.especie() != animal.getEspecie()) {
            throw new RegraNegocioException(animal.getNome() + " é mãe de animais cadastrados e não pode trocar de "
                    + "espécie.");
        }
    }

    private Animal obter(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new AnimalNaoEncontradoException("Animal não encontrado com o id: " + id));
    }

}
