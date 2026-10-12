package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.response.UsuarioNomeResponse;
import com.br.octopus_msusuario.dto.response.UsuarioResumoResponse;
import com.br.octopus_msusuario.service.UsuarioConsultaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Listagem única dos quatro perfis com conta. O cadastro e a desativação continuam em /api/<perfil>.
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuários")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioConsultaService usuarioConsultaService;

    @GetMapping
    public List<UsuarioResumoResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Role perfil,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cadastradoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cadastradoAte) {
        return usuarioConsultaService.listar(nome, perfil, ativo, cadastradoDe, cadastradoAte);
    }

    // Qualquer perfil logado: só e-mail, nome e perfil, inclusive de contas desativadas, que continuam no histórico.
    // Usado pelo octopus-msrelatorios para mostrar quem aplicou uma dose ou registrou uma internação.
    @GetMapping("/nomes")
    @PreAuthorize("isAuthenticated()")
    public List<UsuarioNomeResponse> nomes() {
        return usuarioConsultaService.listar(null, null, null, null, null).stream()
                .map(u -> new UsuarioNomeResponse(u.email(), u.nome(), u.role()))
                .toList();
    }
}
