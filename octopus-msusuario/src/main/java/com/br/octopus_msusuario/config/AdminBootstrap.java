package com.br.octopus_msusuario.config;

import com.br.octopus_msusuario.domain.Admin;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.repository.AdminRepository;
import com.br.octopus_msusuario.repository.UsuarioRepository;
import com.br.octopus_msusuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Só admin cadastra admin, então o primeiro precisa nascer aqui.
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap {

    private final UsuarioRepository usuarioRepository;
    private final AdminRepository adminRepository;
    private final UsuarioService usuarioService;

    @Bean
    ApplicationRunner criarAdminInicial(
            @Value("${app.bootstrap.admin.email}") String email,
            @Value("${app.bootstrap.admin.senha}") String senha,
            @Value("${app.bootstrap.admin.nome}") String nome
    ) {
        return args -> {
            if (usuarioRepository.existsByRole(Role.ROLE_ADMIN)) {
                return;
            }
            Admin admin = Admin.builder()
                    .nome(nome)
                    .usuario(usuarioService.novoUsuario(email, senha, Role.ROLE_ADMIN))
                    .build();
            adminRepository.save(admin);
            log.warn("Admin inicial criado com e-mail '{}'. Troque a senha padrão.", email);
        };
    }
}
