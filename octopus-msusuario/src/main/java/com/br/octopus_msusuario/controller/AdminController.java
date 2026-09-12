package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.request.AdminRequest;
import com.br.octopus_msusuario.dto.response.AdminResponse;
import com.br.octopus_msusuario.service.AdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administradores")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminResponse criar(@Valid @RequestBody AdminRequest request) {
        return adminService.criar(request);
    }

    @GetMapping
    public List<AdminResponse> listar() {
        return adminService.listar();
    }
}
