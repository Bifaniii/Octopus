package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.request.AuxiliarRequest;
import com.br.octopus_msusuario.dto.response.AuxiliarResponse;
import com.br.octopus_msusuario.service.AuxiliarService;
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
@RequestMapping("/api/auxiliares")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auxiliares")
@SecurityRequirement(name = "bearerAuth")
public class AuxiliarController {

    private final AuxiliarService auxiliarService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuxiliarResponse criar(@Valid @RequestBody AuxiliarRequest request) {
        return auxiliarService.criar(request);
    }

    @GetMapping
    public List<AuxiliarResponse> listar() {
        return auxiliarService.listar();
    }

    @GetMapping("/{id}")
    public AuxiliarResponse buscar(@PathVariable UUID id) {
        return auxiliarService.buscar(id);
    }

    @PatchMapping("/{id}/desativar")
    public AuxiliarResponse desativar(@PathVariable UUID id) {
        return auxiliarService.desativar(id);
    }
}
