package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.request.EsqueciSenhaRequest;
import com.br.octopus_msusuario.dto.request.LoginRequest;
import com.br.octopus_msusuario.dto.request.RedefinirSenhaRequest;
import com.br.octopus_msusuario.dto.response.LoginResponse;
import com.br.octopus_msusuario.service.AuthService;
import com.br.octopus_msusuario.service.RecuperacaoSenhaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().body(authService.login(request));
    }

    // Responde 204 mesmo quando o e-mail não tem conta, para não revelar quem está cadastrado.
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        recuperacaoSenhaService.solicitar(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        recuperacaoSenhaService.redefinir(request);
        return ResponseEntity.noContent().build();
    }
}
