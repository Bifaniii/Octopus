package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Veterinario;
import com.br.octopus_msusuario.domain.enums.Especializacao;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.request.VeterinarioRequest;
import com.br.octopus_msusuario.dto.response.VeterinarioResponse;
import com.br.octopus_msusuario.exception.RecursoDuplicadoException;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public VeterinarioResponse criar(VeterinarioRequest request) {
        if (veterinarioRepository.existsByCpfCnpj(request.cpfCnpj())) {
            throw new RecursoDuplicadoException("CPF/CNPJ já cadastrado: " + request.cpfCnpj());
        }
        if (veterinarioRepository.existsByCrmvAndCrmvUf(request.crmv(), request.crmvUf())) {
            throw new RecursoDuplicadoException("CRMV já cadastrado: " + request.crmv() + "/" + request.crmvUf());
        }

        Veterinario veterinario = Veterinario.builder()
                .nome(request.nome())
                .cpfCnpj(request.cpfCnpj())
                .tipoPessoa(request.tipoPessoa())
                .dataNascimento(request.dataNascimento())
                .telefone(request.telefone())
                .crmv(request.crmv())
                .crmvUf(request.crmvUf())
                .especializacao(request.especializacao() != null ? request.especializacao() : Especializacao.GERAL)
                .usuario(usuarioService.novoUsuario(request.email(), request.senha(), Role.ROLE_VETERINARIO))
                .build();

        return VeterinarioResponse.from(veterinarioRepository.save(veterinario));
    }

    @Transactional(readOnly = true)
    public List<VeterinarioResponse> listar() {
        return veterinarioRepository.findAll().stream().map(VeterinarioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VeterinarioResponse buscar(UUID id) {
        return VeterinarioResponse.from(obter(id));
    }

    @Transactional
    public VeterinarioResponse desativar(UUID id) {
        Veterinario veterinario = obter(id);
        veterinario.getUsuario().setAtivo(false);
        return VeterinarioResponse.from(veterinario);
    }

    private Veterinario obter(UUID id) {
        return veterinarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veterinário não encontrado: " + id));
    }
}
