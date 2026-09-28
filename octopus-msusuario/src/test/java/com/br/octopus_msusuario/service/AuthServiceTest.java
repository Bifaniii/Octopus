package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Usuario;
import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.request.LoginRequest;
import com.br.octopus_msusuario.dto.response.LoginResponse;
import com.br.octopus_msusuario.exception.LoginNaoPermitidoException;
import com.br.octopus_msusuario.security.JwtService;
import com.br.octopus_msusuario.security.UsuarioDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private void autenticaComo(Role role) {
        var details = new UsuarioDetails(Usuario.builder().email("user@vidapet.com").senha("hash").role(role).build());
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    void login_deveRetornarTokenBearerComRoleDoUsuario() {
        autenticaComo(Role.ROLE_VETERINARIO);
        Instant expira = Instant.now().plusSeconds(3600);
        when(jwtService.gerarToken("user@vidapet.com", Role.ROLE_VETERINARIO))
                .thenReturn(new JwtService.TokenGerado("jwt", expira));

        LoginResponse response = authService.login(new LoginRequest("user@vidapet.com", "senha-forte"));

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.role()).isEqualTo(Role.ROLE_VETERINARIO);
        assertThat(response.expiraEm()).isEqualTo(expira);
    }

    @Test
    void login_deveBloquearTutorMesmoComSenhaCorreta() {
        autenticaComo(Role.ROLE_TUTOR);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@vidapet.com", "senha-forte")))
                .isInstanceOf(LoginNaoPermitidoException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_devePropagarCredenciaisInvalidas() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@vidapet.com", "errada")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }
}
