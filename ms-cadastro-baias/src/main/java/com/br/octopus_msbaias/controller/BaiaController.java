package com.br.octopus_msbaias.controller;

import com.br.octopus_msbaias.dto.request.BaiaRequest;
import com.br.octopus_msbaias.dto.response.BaiaResponse;
import com.br.octopus_msbaias.service.BaiaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/baias")
@RequiredArgsConstructor
@Tag(name = "Baias")
@SecurityRequirement(name = "bearerAuth")
public class BaiaController {

    private final BaiaService baiaService;

    // Escrita (criar/editar/deletar): apenas ROLE_ADMIN.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public BaiaResponse criar(@Valid @RequestBody BaiaRequest request) {
        return baiaService.criar(request);
    }

    // Leitura (listar/buscar): qualquer usuário autenticado (exigido pela SecurityConfig).
    @GetMapping
    public List<BaiaResponse> listar() {
        return baiaService.listar();
    }

    @GetMapping("/{id}")
    public BaiaResponse buscar(@PathVariable UUID id) {
        return baiaService.buscar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaiaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody BaiaRequest request) {
        return baiaService.atualizar(id, request);
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public BaiaResponse desativar(@PathVariable UUID id) {
        return baiaService.desativar(id);
    }
}
