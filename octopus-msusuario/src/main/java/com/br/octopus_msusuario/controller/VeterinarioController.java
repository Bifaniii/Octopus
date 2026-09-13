package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.request.VeterinarioRequest;
import com.br.octopus_msusuario.dto.response.VeterinarioResponse;
import com.br.octopus_msusuario.service.VeterinarioService;
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
@RequestMapping("/api/veterinarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Veterinários")
@SecurityRequirement(name = "bearerAuth")
public class VeterinarioController {

    private final VeterinarioService veterinarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VeterinarioResponse criar(@Valid @RequestBody VeterinarioRequest request) {
        return veterinarioService.criar(request);
    }

    @GetMapping
    public List<VeterinarioResponse> listar() {
        return veterinarioService.listar();
    }

    @GetMapping("/{id}")
    public VeterinarioResponse buscar(@PathVariable UUID id) {
        return veterinarioService.buscar(id);
    }

    @PatchMapping("/{id}/desativar")
    public VeterinarioResponse desativar(@PathVariable UUID id) {
        return veterinarioService.desativar(id);
    }
}
