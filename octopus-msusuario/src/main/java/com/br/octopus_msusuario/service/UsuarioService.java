package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Usuario;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.exception.RecursoDuplicadoException;
import com.br.octopus_msusuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Usuario novoUsuario(String email, String senha, Role role) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RecursoDuplicadoException("E-mail já cadastrado: " + email);
        }
        return Usuario.builder()
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .role(role)
                .build();
    }
}
