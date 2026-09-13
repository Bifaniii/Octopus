package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.Tutor;
import com.br.octopus_msusuario.dto.request.TutorRequest;
import com.br.octopus_msusuario.dto.response.TutorResponse;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository tutorRepository;

    @Transactional
    public TutorResponse criar(TutorRequest request) {
        Tutor tutor = Tutor.builder()
                .nome(request.nome())
                .endereco(request.endereco())
                .dataNascimento(request.dataNascimento())
                .telefone(request.telefone())
                .build();

        if (request.animais() != null) {
            request.animais().forEach(a -> tutor.adicionarAnimal(Animal.builder().nome(a.nome()).build()));
        }

        return TutorResponse.from(tutorRepository.save(tutor));
    }

    @Transactional(readOnly = true)
    public List<TutorResponse> listar() {
        return tutorRepository.findAll().stream().map(TutorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TutorResponse buscar(UUID id) {
        return TutorResponse.from(obter(id));
    }

    Tutor obter(UUID id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor não encontrado: " + id));
    }
}
