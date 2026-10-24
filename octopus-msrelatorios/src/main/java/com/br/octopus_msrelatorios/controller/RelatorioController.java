package com.br.octopus_msrelatorios.controller;

import com.br.octopus_msrelatorios.domain.SituacaoDose;
import com.br.octopus_msrelatorios.domain.Turno;
import com.br.octopus_msrelatorios.relatorio.ExportadorRelatorio;
import com.br.octopus_msrelatorios.relatorio.Formato;
import com.br.octopus_msrelatorios.service.RelatorioBaiasService;
import com.br.octopus_msrelatorios.service.RelatorioDosesService;
import com.br.octopus_msrelatorios.service.RelatorioInternacoesService;
import com.br.octopus_msrelatorios.service.RelatorioPrescricaoService;
import com.br.octopus_msrelatorios.service.RelatorioTutoresAnimaisService;
import com.br.octopus_msrelatorios.service.RelatorioUsuariosService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Cada relatório sai em PDF ou XLSX (?formato=PDF|XLSX). A permissão é a mesma de quem lê aqueles dados no
// sistema: o token é repassado aos outros módulos, que também conferem.
@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios")
@SecurityRequirement(name = "bearerAuth")
public class RelatorioController {

    private final ExportadorRelatorio exportador;
    private final RelatorioDosesService dosesService;
    private final RelatorioInternacoesService internacoesService;
    private final RelatorioTutoresAnimaisService tutoresAnimaisService;
    private final RelatorioBaiasService baiasService;
    private final RelatorioUsuariosService usuariosService;
    private final RelatorioPrescricaoService prescricaoService;

    // Sem período, o dia de hoje. auxiliar é o e-mail de quem aplicou.
    @GetMapping("/doses")
    public ResponseEntity<byte[]> doses(
            @RequestParam(defaultValue = "PDF") Formato formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Turno turno,
            @RequestParam(required = false) String auxiliar,
            @RequestParam(required = false) SituacaoDose situacao,
            Authentication autenticacao) {
        return exportador.exportar(dosesService.gerar(de, ate, turno, auxiliar, situacao, autenticacao), formato);
    }

    // de/ate filtram a data de admissão.
    @GetMapping("/internacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'VETERINARIO')")
    public ResponseEntity<byte[]> internacoes(
            @RequestParam(defaultValue = "PDF") Formato formato,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            Authentication autenticacao) {
        return exportador.exportar(internacoesService.gerar(status, de, ate, autenticacao), formato);
    }

    @GetMapping("/tutores-animais")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'VETERINARIO')")
    public ResponseEntity<byte[]> tutoresAnimais(@RequestParam(defaultValue = "PDF") Formato formato,
                                                 Authentication autenticacao) {
        return exportador.exportar(tutoresAnimaisService.gerar(autenticacao), formato);
    }

    @GetMapping("/baias")
    public ResponseEntity<byte[]> baias(@RequestParam(defaultValue = "PDF") Formato formato,
                                        Authentication autenticacao) {
        return exportador.exportar(baiasService.gerar(autenticacao), formato);
    }

    // perfil: ROLE_ADMIN, ROLE_VETERINARIO, ROLE_RECEPCIONISTA ou ROLE_AUXILIAR.
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> usuarios(
            @RequestParam(defaultValue = "PDF") Formato formato,
            @RequestParam(required = false) String perfil,
            @RequestParam(required = false) Boolean ativo,
            Authentication autenticacao) {
        return exportador.exportar(usuariosService.gerar(perfil, ativo, autenticacao), formato);
    }

    // Uma prescrição para imprimir, sempre em PDF, com a assinatura de quem prescreveu.
    @GetMapping("/prescricoes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    public ResponseEntity<byte[]> prescricao(@PathVariable Long id, Authentication autenticacao) {
        return exportador.exportar(prescricaoService.gerar(id, autenticacao), Formato.PDF);
    }
}
