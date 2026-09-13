package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Auxiliar;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.request.AuxiliarRequest;
import com.br.octopus_msusuario.dto.response.AuxiliarResponse;
import com.br.octopus_msusuario.exception.RecursoDuplicadoException;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.repository.AuxiliarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuxiliarService {

    private final AuxiliarRepository auxiliarRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public AuxiliarResponse criar(AuxiliarRequest request) {
        if (auxiliarRepository.existsByCpf(request.cpf())) {
            throw new RecursoDuplicadoException("CPF já cadastrado: " + request.cpf());
        }

        Auxiliar auxiliar = Auxiliar.builder()
                .nome(request.nome())
                .cpf(request.cpf())
                .dataNascimento(request.dataNascimento())
                .telefone(request.telefone())
                .usuario(usuarioService.novoUsuario(request.email(), request.senha(), Role.ROLE_AUXILIAR))
                .build();

        return AuxiliarResponse.from(auxiliarRepository.save(auxiliar));
    }

    @Transactional(readOnly = true)
    public List<AuxiliarResponse> listar() {
        return auxiliarRepository.findAll().stream().map(AuxiliarResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AuxiliarResponse buscar(UUID id) {
        return AuxiliarResponse.from(obter(id));
    }

    @Transactional
    public AuxiliarResponse desativar(UUID id) {
        Auxiliar auxiliar = obter(id);
        auxiliar.getUsuario().setAtivo(false);
        return AuxiliarResponse.from(auxiliar);
    }

    private Auxiliar obter(UUID id) {
        return auxiliarRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Auxiliar não encontrado: " + id));
    }
}
