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

    /**
     * Troca o e-mail de login. Os tokens já emitidos levam o e-mail antigo no "sub" e deixam de valer, porque o filtro
     * recarrega o usuário por ele.
     */
    public void alterarEmail(Usuario usuario, String novoEmail) {
        if (usuario.getEmail().equalsIgnoreCase(novoEmail)) {
            usuario.setEmail(novoEmail);
            return;
        }
        if (usuarioRepository.existsByEmail(novoEmail)) {
            throw new RecursoDuplicadoException("E-mail já cadastrado: " + novoEmail);
        }
        usuario.setEmail(novoEmail);
    }

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
