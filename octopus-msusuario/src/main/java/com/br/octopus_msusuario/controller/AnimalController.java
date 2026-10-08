package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.request.AnimalRequest;
import com.br.octopus_msusuario.dto.response.AnimalResponse;
import com.br.octopus_msusuario.service.AnimalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/animais")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
@Tag(name = "Animais")
@SecurityRequirement(name = "bearerAuth")
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping
    public List<AnimalResponse> listar() {
        return animalService.listar();
    }

    // Consumido pelo ms-internacao (RN-01/RN-02), inclusive quando o veterinário transfere para isolamento.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'VETERINARIO')")
    public AnimalResponse buscar(@PathVariable UUID id) {
        return animalService.listarPorId(id);
    }

    @PostMapping
    public ResponseEntity<AnimalResponse> criar(UUID tutorId, AnimalRequest request) {
        return ResponseEntity.ok().body(animalService.criar(tutorId, request));
    }
}
