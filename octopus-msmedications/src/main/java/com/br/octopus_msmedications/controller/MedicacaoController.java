package com.br.octopus_msmedications.controller;

import com.br.octopus_msmedications.dto.request.MedicacaoRequest;
import com.br.octopus_msmedications.dto.request.MedicacaoUpdateRequest;
import com.br.octopus_msmedications.dto.response.MedicacaoResponse;
import com.br.octopus_msmedications.service.MedicacaoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicacoes")
@RequiredArgsConstructor
@Tag(name = "Medicações")
@SecurityRequirement(name = "bearerAuth")
public class MedicacaoController {

    private final MedicacaoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    public MedicacaoResponse criar(@Valid @RequestBody MedicacaoRequest request) {
        return service.criar(request);
    }

    @GetMapping
    public List<MedicacaoResponse> listar(
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String nomeComercial
    ) {
        if (fabricante != null && !fabricante.isBlank()) {
            return service.listarPorFabricante(fabricante);
        }
        if (nomeComercial != null && !nomeComercial.isBlank()) {
            return service.listarPorNomeComercial(nomeComercial);
        }
        return service.listar();
    }

    @GetMapping("/{id}")
    public MedicacaoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    public MedicacaoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody MedicacaoUpdateRequest request) {
        return service.atualizarParcial(id, request);
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public MedicacaoResponse desativar(@PathVariable UUID id) {
        return service.desativar(id);
    }
}
