package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.request.LoginRequest;
import com.br.octopus_msusuario.dto.response.LoginResponse;
import com.br.octopus_msusuario.exception.LoginNaoPermitidoException;
import com.br.octopus_msusuario.security.JwtService;
import com.br.octopus_msusuario.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TIPO_TOKEN = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        UsuarioDetails details = (UsuarioDetails) auth.getPrincipal();
        if (details.getRole() == Role.ROLE_TUTOR) {
            throw new LoginNaoPermitidoException("Tutores não podem acessar o sistema");
        }

        JwtService.TokenGerado gerado = jwtService.gerarToken(details.getUsername(), details.getRole());
        return new LoginResponse(gerado.token(), TIPO_TOKEN, gerado.expiraEm(), details.getUsername(), details.getRole());
    }
}
