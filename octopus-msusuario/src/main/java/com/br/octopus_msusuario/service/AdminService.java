package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Admin;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.request.AdminRequest;
import com.br.octopus_msusuario.dto.response.AdminResponse;
import com.br.octopus_msusuario.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
