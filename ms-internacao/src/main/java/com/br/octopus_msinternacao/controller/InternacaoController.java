package com.br.octopus_msinternacao.controller;


import com.br.octopus_msinternacao.domain.enums.StatusInternacao;
import com.br.octopus_msinternacao.dto.request.AdmissaoRequest;
import com.br.octopus_msinternacao.dto.request.AltaAPedidoRequest;
import com.br.octopus_msinternacao.dto.request.EncerramentoRequest;
import com.br.octopus_msinternacao.dto.request.IsolamentoRequest;
import com.br.octopus_msinternacao.dto.response.InternacaoEventoResponse;
import com.br.octopus_msinternacao.dto.response.InternacaoResponse;
import com.br.octopus_msinternacao.service.InternacaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

// Não há DELETE: internação não se apaga, termina em ENCERRADA. O usuário de cada operação é o e-mail do token
// (o JwtAuthenticationFilter põe o e-mail como principal) e fica gravado no histórico.
@RestController
@RequestMapping("/api/internacoes")
@RequiredArgsConstructor
@Tag(name = "Internações")
@SecurityRequirement(name = "bearerAuth")
public class InternacaoController {

    private final InternacaoService internacaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMIN')")
    public InternacaoResponse admitir(@Valid @RequestBody AdmissaoRequest request,
                                      @AuthenticationPrincipal String usuario) {
        return internacaoService.admitir(request, usuario);
    }

    // Leitura: qualquer usuário autenticado (exigido pela SecurityConfig). Filtros opcionais.
    @GetMapping
    public List<InternacaoResponse> listar(@RequestParam(required = false) StatusInternacao status,
                                           @RequestParam(required = false) UUID baiaId) {
        return internacaoService.listar(status, baiaId);
    }

    @GetMapping("/{id}")
    public InternacaoResponse buscar(@PathVariable UUID id) {
        return internacaoService.buscar(id);
    }

    @GetMapping("/{id}/eventos")
    public List<InternacaoEventoResponse> eventos(@PathVariable UUID id) {
        return internacaoService.eventos(id);
    }

    @PatchMapping("/{id}/iniciar-tratamento")
    @PreAuthorize("hasRole('VETERINARIO')")
    public InternacaoResponse iniciarTratamento(@PathVariable UUID id, @AuthenticationPrincipal String usuario) {
        return internacaoService.iniciarTratamento(id, usuario);
    }

    @PatchMapping("/{id}/isolar")
    @PreAuthorize("hasRole('VETERINARIO')")
    public InternacaoResponse isolar(@PathVariable UUID id, @Valid @RequestBody IsolamentoRequest request,
                                     @AuthenticationPrincipal String usuario) {
        return internacaoService.isolar(id, request, usuario);
    }

    @PatchMapping("/{id}/autorizar-alta")
    @PreAuthorize("hasRole('VETERINARIO')")
    public InternacaoResponse autorizarAlta(@PathVariable UUID id, @AuthenticationPrincipal String usuario) {
        return internacaoService.autorizarAlta(id, usuario);
    }

    @PatchMapping("/{id}/alta-a-pedido-do-tutor")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'VETERINARIO')")
    public InternacaoResponse altaAPedidoDoTutor(@PathVariable UUID id, @Valid @RequestBody AltaAPedidoRequest request,
                                                 @AuthenticationPrincipal String usuario) {
        return internacaoService.altaAPedidoDoTutor(id, request, usuario);
    }

    // RN-08: registra a saída física e libera a baia.
    @PatchMapping("/{id}/encerrar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'VETERINARIO')")
    public InternacaoResponse encerrar(@PathVariable UUID id, @Valid @RequestBody EncerramentoRequest request,
                                       @AuthenticationPrincipal String usuario) {
        return internacaoService.encerrar(id, request, usuario);
    }
}
