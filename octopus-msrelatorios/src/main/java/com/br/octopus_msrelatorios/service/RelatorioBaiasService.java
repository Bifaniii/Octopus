package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.client.BaiaClient;
import com.br.octopus_msrelatorios.client.InternacaoClient;
import com.br.octopus_msrelatorios.client.dto.BaiaDto;
import com.br.octopus_msrelatorios.client.dto.InternacaoDto;
import com.br.octopus_msrelatorios.domain.Rotulos;
import com.br.octopus_msrelatorios.relatorio.Relatorio;
import com.br.octopus_msrelatorios.relatorio.Valores;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.br.octopus_msrelatorios.relatorio.Relatorio.coluna;

/** Ocupação atual das baias. Ocupa a baia toda internação que ainda não foi encerrada (RN-08). */
@Service
@RequiredArgsConstructor
public class RelatorioBaiasService {

    private static final List<String> TIPOS = List.of("COLETIVA", "ISOLAMENTO", "NINHADA");

    private final BaiaClient baiaClient;
    private final InternacaoClient internacaoClient;
    private final Pessoas pessoas;
    private final Clock clock;

    public Relatorio gerar(Authentication autenticacao) {
        Pessoas.Nomes nomes = pessoas.carregar();
        List<BaiaDto> baias = baiaClient.listar().stream()
                .sorted(Comparator.comparing(BaiaDto::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<Long, List<InternacaoDto>> ocupantes = internacaoClient.listar().stream()
                .filter(InternacaoDto::ocupaBaia)
                .collect(Collectors.groupingBy(InternacaoDto::baiaId));

        List<List<Object>> linhas = new ArrayList<>();
        for (BaiaDto baia : baias) {
            List<InternacaoDto> nela = ocupantes.getOrDefault(baia.id(), List.of());
            String animais = nela.stream()
                    .map(i -> i.animalNome() + " (" + Rotulos.statusInternacao(i.status()).toLowerCase() + ")")
                    .collect(Collectors.joining(", "));
            linhas.add(List.of(baia.nome(), Rotulos.tipoBaia(baia.tipo()), (long) baia.capacidade(),
                    (long) nela.size(), (long) Math.max(0, baia.capacidade() - nela.size()), animais,
                    baia.ativo() ? "Ativa" : "Desativada"));
        }

        List<List<Object>> porTipo = new ArrayList<>();
        for (String tipo : TIPOS) {
            List<BaiaDto> doTipo = baias.stream().filter(b -> b.ativo() && tipo.equals(b.tipo())).toList();
            long capacidade = doTipo.stream().mapToLong(BaiaDto::capacidade).sum();
            long ocupadas = doTipo.stream().mapToLong(b -> ocupantes.getOrDefault(b.id(), List.of()).size()).sum();
            porTipo.add(List.of(Rotulos.tipoBaia(tipo), (long) doTipo.size(), capacidade, ocupadas,
                    Math.max(0, capacidade - ocupadas)));
        }

        long ativas = baias.stream().filter(BaiaDto::ativo).count();
        return new Relatorio(
                "Relatório de ocupação de baias",
                "relatorio-baias",
                List.of("Situação em " + Valores.texto(LocalDateTime.now(clock)),
                        ativas + " baias ativas de " + baias.size()),
                List.of(
                        new Relatorio.Tabela("Por tipo (baias ativas)", List.of(
                                coluna("Tipo", 2), coluna("Baias", 1), coluna("Vagas totais", 1),
                                coluna("Animais", 1), coluna("Vagas livres", 1)), porTipo),
                        new Relatorio.Tabela("Baias", List.of(
                                coluna("Baia", 0.8f), coluna("Tipo", 1.1f), coluna("Capacidade", 1.3f),
                                coluna("Animais", 0.9f), coluna("Vagas", 0.8f), coluna("Internados", 3f),
                                coluna("Situação", 1.1f)), linhas)),
                nomes.emissor(autenticacao),
                LocalDateTime.now(clock),
                false);
    }
}
