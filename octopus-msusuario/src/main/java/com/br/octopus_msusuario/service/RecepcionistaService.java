package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Recepcionista;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.request.RecepcionistaRequest;
import com.br.octopus_msusuario.dto.response.RecepcionistaResponse;
import com.br.octopus_msusuario.exception.RecursoDuplicadoException;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.repository.RecepcionistaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecepcionistaService {

    private final RecepcionistaRepository recepcionistaRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public RecepcionistaResponse criar(RecepcionistaRequest request) {
        if (recepcionistaRepository.existsByCpf(request.cpf())) {
            throw new RecursoDuplicadoException("CPF já cadastrado: " + request.cpf());
        }

        Recepcionista recepcionista = Recepcionista.builder()
                .nome(request.nome())
                .cpf(request.cpf())
                .dataNascimento(request.dataNascimento())
                .telefone(request.telefone())
                .usuario(usuarioService.novoUsuario(request.email(), request.senha(), Role.ROLE_RECEPCIONISTA))
                .build();

        return RecepcionistaResponse.from(recepcionistaRepository.save(recepcionista));
    }

    @Transactional(readOnly = true)
    public List<RecepcionistaResponse> listar() {
        return recepcionistaRepository.findAll().stream().map(RecepcionistaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RecepcionistaResponse buscar(UUID id) {
        return RecepcionistaResponse.from(obter(id));
    }

    @Transactional
    public RecepcionistaResponse desativar(UUID id) {
        Recepcionista recepcionista = obter(id);
        recepcionista.getUsuario().setAtivo(false);
        return RecepcionistaResponse.from(recepcionista);
    }

    private Recepcionista obter(UUID id) {
        return recepcionistaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recepcionista não encontrada: " + id));
    }
}
