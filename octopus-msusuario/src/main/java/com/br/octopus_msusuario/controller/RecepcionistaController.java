package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.request.RecepcionistaRequest;
import com.br.octopus_msusuario.dto.response.RecepcionistaResponse;
import com.br.octopus_msusuario.service.RecepcionistaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recepcionistas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Recepcionistas")
@SecurityRequirement(name = "bearerAuth")
public class RecepcionistaController {

    private final RecepcionistaService recepcionistaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecepcionistaResponse criar(@Valid @RequestBody RecepcionistaRequest request) {
        return recepcionistaService.criar(request);
    }

    @GetMapping
    public List<RecepcionistaResponse> listar() {
        return recepcionistaService.listar();
    }

    @GetMapping("/{id}")
    public RecepcionistaResponse buscar(@PathVariable UUID id) {
        return recepcionistaService.buscar(id);
    }

    @PatchMapping("/{id}/desativar")
    public RecepcionistaResponse desativar(@PathVariable UUID id) {
        return recepcionistaService.desativar(id);
    }
}
