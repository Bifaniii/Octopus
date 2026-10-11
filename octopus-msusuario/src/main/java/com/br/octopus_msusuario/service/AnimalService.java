package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.Tutor;
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
        Animal animal = request.toEntity(animalRepository.buscarMae(request.maeId()));
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
        exigirMaeValida(animal, mae);

        animal.setNome(request.nome());
        animal.setEspecie(request.especie());
        animal.setDataUltimaAntirrabica(request.dataAntirrabica());
        animal.setMae(mae);
        return AnimalResponse.from(animal);
    }

    // Subindo pela linha de mães a partir da mãe escolhida, não pode aparecer o próprio animal.
    private void exigirMaeValida(Animal animal, Animal mae) {
        for (Animal atual = mae; atual != null; atual = atual.getMae()) {
            if (atual.getId().equals(animal.getId())) {
                throw new RegraNegocioException("Um animal não pode ser mãe de si mesmo nem da própria ascendência.");
            }
        }
    }

    private Animal obter(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new AnimalNaoEncontradoException("Animal não encontrado com o id: " + id));
    }

}
