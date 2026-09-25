package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.Tutor;
import com.br.octopus_msusuario.dto.request.AnimalRequest;
import com.br.octopus_msusuario.dto.response.AnimalResponse;
import com.br.octopus_msusuario.exception.AnimalNaoEncontradoException;
import com.br.octopus_msusuario.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final TutorService tutorService;

    @Transactional
    public AnimalResponse criar(UUID tutorId, AnimalRequest request) {
        Tutor tutor = tutorService.obter(tutorId);
        Animal animal = Animal.builder().nome(request.nome()).build();
        tutor.adicionarAnimal(animal);
        return AnimalResponse.from(animalRepository.save(animal));
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> listar() {
        return animalRepository.findAll().stream().map(AnimalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AnimalResponse listarPorId(UUID id) throws AnimalNaoEncontradoException{
        Animal animal = animalRepository.findById(id)
            .orElseThrow(() -> new AnimalNaoEncontradoException("Animal não encontrado com o id: " + id));
        return AnimalResponse.from(animal);
    }

    @Transactional
    public AnimalResponse criar(AnimalRequest request) {
        Animal animal = Animal.builder()
        .nome(request.nome())
        .tutor(request.tutor())
        .especie(request.especie())
        .dataUltimaAntirrabica(request.dataAntirrabica())
        .build();

        return AnimalResponse.from(animal);
    }
}
