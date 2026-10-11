package com.br.octopus_msprescricao.controller;

import com.br.octopus_msprescricao.dto.request.PrescricaoRequest;
import com.br.octopus_msprescricao.dto.response.PrescricaoResponse;
import com.br.octopus_msprescricao.service.PrescricaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Leitura: qualquer perfil autenticado (o auxiliar precisa ver o que aplicar). Escrita: só VETERINARIO, que é quem
// prescreve (enunciado) e o único perfil que o ms-internacao aceita no iniciar-tratamento.
@RestController
@RequestMapping("/api/prescricoes")
@RequiredArgsConstructor
@Tag(name = "Prescrições")
@SecurityRequirement(name = "bearerAuth")
public class PrescricaoController {

    private final PrescricaoService prescricaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VETERINARIO')")
    public PrescricaoResponse criar(@Valid @RequestBody PrescricaoRequest request, Authentication authentication) {
        // O principal é o e-mail do claim "sub" (ver JwtAuthenticationFilter).
        return prescricaoService.criar(request, authentication.getName());
    }

    @GetMapping
    public List<PrescricaoResponse> listar(@RequestParam(required = false) Long internacaoId) {
        return prescricaoService.listar(internacaoId);
    }

    @GetMapping("/{id}")
    public PrescricaoResponse buscar(@PathVariable Long id) {
        return prescricaoService.buscar(id);
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('VETERINARIO')")
    public PrescricaoResponse desativar(@PathVariable Long id) {
        return prescricaoService.desativar(id);
    }
}
