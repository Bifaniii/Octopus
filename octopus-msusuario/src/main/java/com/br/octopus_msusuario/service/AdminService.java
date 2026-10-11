package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Admin;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.request.AdminRequest;
import com.br.octopus_msusuario.dto.request.AdminUpdateRequest;
import com.br.octopus_msusuario.dto.response.AdminResponse;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public AdminResponse criar(AdminRequest request) {
        Admin admin = Admin.builder()
                .nome(request.nome())
                .usuario(usuarioService.novoUsuario(request.email(), request.senha(), Role.ROLE_ADMIN))
                .build();
        return AdminResponse.from(adminRepository.save(admin));
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> listar() {
        return adminRepository.findAll().stream().map(AdminResponse::from).toList();
    }

    @Transactional
    public AdminResponse atualizar(UUID id, AdminUpdateRequest request) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador não encontrado: " + id));
        usuarioService.alterarEmail(admin.getUsuario(), request.email());
        admin.setNome(request.nome());
        return AdminResponse.from(admin);
    }
}
