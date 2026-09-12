package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.response.AnimalResponse;
import com.br.octopus_msusuario.service.AnimalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<AnimalResponse> listar() {
        return animalService.listar();
    }
}
