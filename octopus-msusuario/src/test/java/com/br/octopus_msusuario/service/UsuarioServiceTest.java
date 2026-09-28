package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Usuario;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.exception.RecursoDuplicadoException;
import com.br.octopus_msusuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void novoUsuario_deveCriarAtivoComSenhaCriptografada() {
        when(usuarioRepository.existsByEmail("aux@vidapet.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-forte")).thenReturn("hash-bcrypt");

        Usuario usuario = usuarioService.novoUsuario("aux@vidapet.com", "senha-forte", Role.ROLE_AUXILIAR);

        assertThat(usuario.getSenha()).isEqualTo("hash-bcrypt");
        assertThat(usuario.getRole()).isEqualTo(Role.ROLE_AUXILIAR);
        assertThat(usuario.isAtivo()).isTrue();
    }

    @Test
    void novoUsuario_deveRejeitarEmailDuplicado() {
        when(usuarioRepository.existsByEmail("aux@vidapet.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.novoUsuario("aux@vidapet.com", "senha-forte", Role.ROLE_AUXILIAR))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("aux@vidapet.com");
        verifyNoInteractions(passwordEncoder);
    }
}
