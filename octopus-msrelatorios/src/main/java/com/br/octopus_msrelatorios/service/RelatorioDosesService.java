package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.client.BaiaClient;
import com.br.octopus_msrelatorios.client.DoseClient;
import com.br.octopus_msrelatorios.client.InternacaoClient;
import com.br.octopus_msrelatorios.client.MedicacaoClient;
import com.br.octopus_msrelatorios.client.dto.BaiaDto;
import com.br.octopus_msrelatorios.client.dto.DoseDto;
import com.br.octopus_msrelatorios.client.dto.InternacaoDto;
import com.br.octopus_msrelatorios.client.dto.MedicacaoDto;
import com.br.octopus_msrelatorios.domain.SituacaoDose;
import com.br.octopus_msrelatorios.domain.Turno;
import com.br.octopus_msrelatorios.domain.Turnos;
import com.br.octopus_msrelatorios.relatorio.Relatorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.br.octopus_msrelatorios.relatorio.Relatorio.coluna;

/** Tela 7 do enunciado: doses aplicadas, atrasadas e perdidas, por turno e por auxiliar. */
@Service
@RequiredArgsConstructor
public class RelatorioDosesService {

    private final DoseClient doseClient;
    private final InternacaoClient internacaoClient;
    private final BaiaClient baiaClient;
    private final MedicacaoClient medicacaoClient;
    private final Pessoas pessoas;
    private final Turnos turnos;
    private final Clock clock;

    public Relatorio gerar(LocalDate de, LocalDate ate, Turno turno, String auxiliar, SituacaoDose situacao,
                           Authentication autenticacao) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = de != null ? de : (ate != null ? ate : hoje);
        LocalDate fim = ate != null ? ate : (de != null ? de : hoje);
        Periodos.validar(inicio, fim);

        LocalDateTime agora = LocalDateTime.now(clock);
        Pessoas.Nomes nomes = pessoas.carregar();
        Map<Long, InternacaoDto> internacoes = porId(internacaoClient.listar(), InternacaoDto::id);
        Map<Long, BaiaDto> baias = porId(baiaClient.listar(), BaiaDto::id);
        Map<Long, MedicacaoDto> medicacoes = porId(medicacaoClient.listar(), MedicacaoDto::id);

        List<Linha> linhas = doseClient.listar(inicio, fim).stream()
                .map(dose -> new Linha(dose, SituacaoDose.de(dose, agora), turnos.de(dose.horarioPrevisto().toLocalTime())))
                .filter(l -> turno == null || l.turno() == turno)
                .filter(l -> situacao == null || l.situacao() == situacao)
                .filter(l -> auxiliar == null || auxiliar.isBlank() || auxiliar.equalsIgnoreCase(l.dose().aplicadaPor()))
                .sorted(Comparator.comparing((Linha l) -> l.dose().horarioPrevisto()).thenComparing(l -> l.dose().id()))
                .toList();

        List<List<Object>> doses = new ArrayList<>();
        for (Linha l : linhas) {
            DoseDto dose = l.dose();
            InternacaoDto internacao = internacoes.get(dose.internacaoId());
            BaiaDto baia = internacao == null ? null : baias.get(internacao.baiaId());
            MedicacaoDto medicacao = medicacoes.get(dose.medicacaoId());
            doses.add(Arrays.asList(
                    dose.horarioPrevisto(),
                    l.turno().rotulo(),
                    internacao == null ? "Internação " + dose.internacaoId() : internacao.animalNome(),
                    baia == null ? null : baia.nome(),
                    medicacao == null ? "Medicamento " + dose.medicacaoId() : medicacao.nomeComercial(),
                    l.situacao().rotulo(),
                    dose.aplicadaEm(),
                    nomes.de(dose.aplicadaPor()),
                    dose.observacao()));
        }

        List<String> filtros = new ArrayList<>();
        filtros.add(Periodos.descricao(inicio, fim));
        filtros.add("Turno: " + (turno == null ? "todos" : turnos.descricao(turno)));
        if (auxiliar != null && !auxiliar.isBlank()) {
            filtros.add("Auxiliar: " + nomes.de(auxiliar));
        }
        if (situacao != null) {
            filtros.add("Situação: " + situacao.rotulo());
        }
        filtros.add("Atrasada: passou do horário, ainda dentro da janela de " + SituacaoDose.JANELA_MINUTOS
                + " min. Perdida: fora da janela.");

        return new Relatorio(
                "Relatório de doses",
                "relatorio-doses",
                filtros,
                List.of(
                        new Relatorio.Tabela("Por turno", List.of(
                                coluna("Turno", 3), coluna("Aplicadas", 1.4f), coluna("Atrasadas", 1.4f),
                                coluna("Perdidas", 1.4f), coluna("Previstas", 1.4f), coluna("Canceladas", 1.4f),
                                coluna("Total", 1.2f)), porTurno(linhas)),
                        new Relatorio.Tabela("Por auxiliar", List.of(
                                coluna("Auxiliar", 3), coluna("E-mail", 3), coluna("Doses aplicadas", 1.5f)),
                                porAuxiliar(linhas, nomes)),
                        new Relatorio.Tabela("Doses", List.of(
                                coluna("Horário previsto", 1.6f), coluna("Turno", 0.9f), coluna("Animal", 1.4f),
                                coluna("Baia", 0.9f), coluna("Medicamento", 1.8f), coluna("Situação", 1.1f),
                                coluna("Aplicada em", 1.6f), coluna("Auxiliar", 1.8f), coluna("Observação", 2.2f)),
                                doses)),
                nomes.emissor(autenticacao),
                agora,
                true);
    }

    private List<List<Object>> porTurno(List<Linha> linhas) {
        List<List<Object>> resultado = new ArrayList<>();
        for (Turno t : Turno.values()) {
            Map<SituacaoDose, Long> contagem = new EnumMap<>(SituacaoDose.class);
            linhas.stream().filter(l -> l.turno() == t)
                    .forEach(l -> contagem.merge(l.situacao(), 1L, Long::sum));
            long total = contagem.values().stream().mapToLong(Long::longValue).sum();
            resultado.add(List.of(turnos.descricao(t),
                    contagem.getOrDefault(SituacaoDose.APLICADA, 0L),
                    contagem.getOrDefault(SituacaoDose.ATRASADA, 0L),
                    contagem.getOrDefault(SituacaoDose.PERDIDA, 0L),
                    contagem.getOrDefault(SituacaoDose.PREVISTA, 0L),
                    contagem.getOrDefault(SituacaoDose.CANCELADA, 0L),
                    total));
        }
        return resultado;
    }

    private static List<List<Object>> porAuxiliar(List<Linha> linhas, Pessoas.Nomes nomes) {
        Map<String, Long> aplicadas = linhas.stream()
                .filter(l -> l.situacao() == SituacaoDose.APLICADA && l.dose().aplicadaPor() != null)
                .collect(Collectors.groupingBy(l -> l.dose().aplicadaPor().toLowerCase(), TreeMap::new,
                        Collectors.counting()));
        return aplicadas.entrySet().stream()
                .map(e -> List.<Object>of(nomes.de(e.getKey()), e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(l -> l.get(0).toString(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static <T> Map<Long, T> porId(List<T> lista, Function<T, Long> id) {
        return lista.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a));
    }

    private record Linha(DoseDto dose, SituacaoDose situacao, Turno turno) {
    }
}
