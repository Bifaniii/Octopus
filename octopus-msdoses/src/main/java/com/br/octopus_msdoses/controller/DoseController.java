package com.br.octopus_msdoses.controller;

import com.br.octopus_msdoses.domain.enums.StatusDose;
import com.br.octopus_msdoses.dto.request.AgendamentoRequest;
import com.br.octopus_msdoses.dto.request.AplicacaoRequest;
import com.br.octopus_msdoses.dto.response.DoseResponse;
import com.br.octopus_msdoses.dto.response.PainelResponse;
import com.br.octopus_msdoses.service.DoseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Leitura: qualquer perfil autenticado. Agendar e cancelar: VETERINARIO, cujo token o msprescricao repassa ao
// gravar ou desativar uma prescrição. Cancelar da internação: quem registra a saída do animal no ms-internacao.
// Aplicar: só AUXILIAR (enunciado: o auxiliar registra a aplicação).
@RestController
@RequestMapping("/api/doses")
@RequiredArgsConstructor
@Tag(name = "Doses")
@SecurityRequirement(name = "bearerAuth")
public class DoseController {

    private final DoseService doseService;

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VETERINARIO')")
    public List<DoseResponse> agendar(@Valid @RequestBody AgendamentoRequest request) {
        return doseService.agendar(request);
    }

    @PatchMapping("/prescricoes/{prescricaoId}/cancelar")
    @PreAuthorize("hasRole('VETERINARIO')")
    public List<DoseResponse> cancelarDaPrescricao(@PathVariable Long prescricaoId) {
        return doseService.cancelarDaPrescricao(prescricaoId);
    }

    // Reativar a prescrição: as doses canceladas ainda dentro da janela voltam a ficar pendentes.
    @PatchMapping("/prescricoes/{prescricaoId}/reativar")
    @PreAuthorize("hasRole('VETERINARIO')")
    public List<DoseResponse> reativarDaPrescricao(@PathVariable Long prescricaoId) {
        return doseService.reativarDaPrescricao(prescricaoId);
    }

    // Chamado pelo ms-internacao ao encerrar (saída física): mesmos perfis que registram a saída lá.
    @PatchMapping("/internacoes/{internacaoId}/cancelar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'VETERINARIO')")
    public List<DoseResponse> cancelarDaInternacao(@PathVariable Long internacaoId) {
        return doseService.cancelarDaInternacao(internacaoId);
    }

    @GetMapping
    public List<DoseResponse> listar(
            @RequestParam(required = false) Long internacaoId,
            @RequestParam(required = false) Long prescricaoId,
            @RequestParam(required = false) StatusDose status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return doseService.listar(internacaoId, prescricaoId, status, de, ate);
    }

    // Tela 6. Vem antes de /{id} só por leitura; o Spring prefere o caminho literal de qualquer jeito.
    @GetMapping("/painel")
    public PainelResponse painel(@RequestParam(required = false) Long internacaoId) {
        return doseService.painel(internacaoId);
    }

    @GetMapping("/{id}")
    public DoseResponse buscar(@PathVariable Long id) {
        return doseService.buscar(id);
    }

    @PatchMapping("/{id}/aplicar")
    @PreAuthorize("hasRole('AUXILIAR')")
    public DoseResponse aplicar(@PathVariable Long id, @Valid @RequestBody(required = false) AplicacaoRequest request,
                                Authentication authentication) {
        return doseService.aplicar(id, authentication.getName(), request == null ? null : request.observacao());
    }

    // Item "se necessário" (mudança contratada de 01/11): cada aplicação cria a dose, respeitando o intervalo mínimo.
    @PostMapping("/se-necessario/{itemPrescricaoId}/aplicacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AUXILIAR')")
    public DoseResponse aplicarSeNecessario(@PathVariable Long itemPrescricaoId,
                                            @Valid @RequestBody(required = false) AplicacaoRequest request,
                                            Authentication authentication) {
        return doseService.aplicarSeNecessario(itemPrescricaoId, authentication.getName(),
                request == null ? null : request.observacao());
    }
}
