package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.client.UsuarioClient;
import com.br.octopus_msrelatorios.client.dto.UsuarioDto;
import com.br.octopus_msrelatorios.domain.Rotulos;
import com.br.octopus_msrelatorios.exception.FiltroInvalidoException;
import com.br.octopus_msrelatorios.relatorio.Relatorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.br.octopus_msrelatorios.relatorio.Relatorio.coluna;

/** Contas do sistema por perfil e situação. Só o administrador gera, como só ele lista usuários. */
@Service
@RequiredArgsConstructor
public class RelatorioUsuariosService {

    private static final List<String> PERFIS = List.of("ROLE_ADMIN", "ROLE_VETERINARIO", "ROLE_RECEPCIONISTA",
            "ROLE_AUXILIAR");

    private final UsuarioClient usuarioClient;
    private final Pessoas pessoas;
    private final Clock clock;

    public Relatorio gerar(String perfil, Boolean ativo, Authentication autenticacao) {
        if (perfil != null && !PERFIS.contains(perfil)) {
            throw new FiltroInvalidoException("Perfil inválido: " + perfil);
        }
        Pessoas.Nomes nomes = pessoas.carregar();
        List<UsuarioDto> usuarios = usuarioClient.usuarios(perfil, ativo);

        List<List<Object>> linhas = usuarios.stream()
                .map(u -> List.<Object>of(u.nome(), u.email(), Rotulos.perfil(u.role()), u.ativo() ? "Ativo" : "Desativado",
                        u.criadoEm()))
                .toList();

        List<List<Object>> porPerfil = new ArrayList<>();
        for (String p : PERFIS) {
            if (perfil != null && !perfil.equals(p)) {
                continue;
            }
            long ativos = usuarios.stream().filter(u -> p.equals(u.role()) && u.ativo()).count();
            long inativos = usuarios.stream().filter(u -> p.equals(u.role()) && !u.ativo()).count();
            porPerfil.add(List.of(Rotulos.perfil(p), ativos, inativos, ativos + inativos));
        }

        return new Relatorio(
                "Relatório de usuários",
                "relatorio-usuarios",
                List.of("Perfil: " + (perfil == null ? "todos" : Rotulos.perfil(perfil)),
                        "Situação: " + (ativo == null ? "todas" : ativo ? "ativos" : "desativados")),
                List.of(
                        new Relatorio.Tabela("Por perfil", List.of(
                                coluna("Perfil", 2), coluna("Ativos", 1), coluna("Desativados", 1), coluna("Total", 1)),
                                porPerfil),
                        new Relatorio.Tabela("Usuários", List.of(
                                coluna("Nome", 2), coluna("E-mail", 2.2f), coluna("Perfil", 1.2f),
                                coluna("Situação", 1), coluna("Cadastrado em", 1.3f)), linhas)),
                nomes.emissor(autenticacao),
                LocalDateTime.now(clock),
                false);
    }
}
