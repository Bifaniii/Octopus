package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.request.AnimalRequest;
import com.br.octopus_msusuario.dto.response.AnimalResponse;
import com.br.octopus_msusuario.service.AnimalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/animais")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
@Tag(name = "Animais")
@SecurityRequirement(name = "bearerAuth")
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'VETERINARIO')")
    public List<AnimalResponse> listar() {
        return animalService.listar();
    }

    // Consumido pelo ms-internacao (RN-01/RN-02), inclusive quando o veterinário transfere para isolamento.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'VETERINARIO')")
    public AnimalResponse buscar(@PathVariable Long id) {
        return animalService.listarPorId(id);
    }

    // Mesmo efeito de POST /api/tutores/{id}/animais, com o tutor na query string.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnimalResponse criar(@RequestParam Long tutorId, @Valid @RequestBody AnimalRequest request) {
        return animalService.criar(tutorId, request);
    }

    // Corrige os dados do animal, inclusive a mãe e a data da última antirrábica. O tutor não muda.
    @PutMapping("/{id}")
    public AnimalResponse atualizar(@PathVariable Long id, @Valid @RequestBody AnimalRequest request) {
        return animalService.atualizar(id, request);
    }
}
