package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.client.BaiaClient;
import com.br.octopus_msrelatorios.client.InternacaoClient;
import com.br.octopus_msrelatorios.client.UsuarioClient;
import com.br.octopus_msrelatorios.client.dto.BaiaDto;
import com.br.octopus_msrelatorios.client.dto.InternacaoDto;
import com.br.octopus_msrelatorios.client.dto.TutorDto;
import com.br.octopus_msrelatorios.domain.Rotulos;
import com.br.octopus_msrelatorios.exception.FiltroInvalidoException;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.br.octopus_msrelatorios.relatorio.Relatorio.coluna;

/** Internações com o animal, o tutor, a baia e quem registrou, filtradas pela data de admissão. */
@Service
@RequiredArgsConstructor
public class RelatorioInternacoesService {

    private static final List<String> STATUS = List.of("ADMITIDA", "EM_TRATAMENTO", "ISOLAMENTO",
            "ALTA_AUTORIZADA", "ALTA_A_PEDIDO_DO_TUTOR", "ENCERRADA");

    private final InternacaoClient internacaoClient;
    private final BaiaClient baiaClient;
    private final UsuarioClient usuarioClient;
    private final Pessoas pessoas;
    private final Clock clock;

    public Relatorio gerar(String status, LocalDate de, LocalDate ate, Authentication autenticacao) {
        if (status != null && !STATUS.contains(status)) {
            throw new FiltroInvalidoException("Status de internação inválido: " + status);
        }
        Periodos.validar(de, ate);

        Pessoas.Nomes nomes = pessoas.carregar();
        Map<Long, BaiaDto> baias = baiaClient.listar().stream()
                .collect(Collectors.toMap(BaiaDto::id, Function.identity(), (a, b) -> a));
        Map<Long, TutorDto> tutorPorAnimal = new HashMap<>();
        for (TutorDto tutor : usuarioClient.tutores()) {
            if (tutor.animais() != null) {
                tutor.animais().forEach(a -> tutorPorAnimal.put(a.id(), tutor));
            }
        }

        List<InternacaoDto> internacoes = internacaoClient.listar().stream()
                .filter(i -> status == null || status.equals(i.status()))
                .filter(i -> Periodos.dentro(i.dataAdmissao().toLocalDate(), de, ate))
                .sorted(Comparator.comparing(InternacaoDto::dataAdmissao).reversed())
                .toList();

        List<List<Object>> linhas = new ArrayList<>();
        Map<String, Long> porStatus = new LinkedHashMap<>();
        STATUS.forEach(s -> porStatus.put(s, 0L));
        for (InternacaoDto i : internacoes) {
            TutorDto tutor = tutorPorAnimal.get(i.animalId());
            BaiaDto baia = baias.get(i.baiaId());
            linhas.add(Arrays.asList(
                    i.id(),
                    i.animalNome(),
                    i.animalEspecie(),
                    tutor == null ? null : tutor.nome(),
                    tutor == null ? null : tutor.telefone(),
                    baia == null ? "Baia " + i.baiaId() : baia.nome(),
                    Rotulos.statusInternacao(i.status()),
                    i.motivo(),
                    i.dataAdmissao(),
                    i.dataAlta(),
                    i.dataSaida(),
                    nomes.de(i.registradoPor())));
            porStatus.merge(i.status(), 1L, Long::sum);
        }

        List<List<Object>> resumo = porStatus.entrySet().stream()
                .filter(e -> status == null || e.getKey().equals(status))
                .map(e -> List.<Object>of(Rotulos.statusInternacao(e.getKey()), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
        resumo.add(List.of("Total", (long) internacoes.size()));

        List<String> filtros = new ArrayList<>();
        filtros.add(Periodos.descricao(de, ate).replace("Período", "Admissão"));
        filtros.add("Situação: " + (status == null ? "todas" : Rotulos.statusInternacao(status)));

        return new Relatorio(
                "Relatório de internações",
                "relatorio-internacoes",
                filtros,
                List.of(
                        new Relatorio.Tabela("Por situação", List.of(coluna("Situação", 3), coluna("Internações", 1)),
                                resumo),
                        new Relatorio.Tabela("Internações", List.of(
                                coluna("Nº", 0.5f), coluna("Animal", 1.2f), coluna("Espécie", 0.9f),
                                coluna("Tutor", 1.5f), coluna("Telefone", 1.2f), coluna("Baia", 0.8f),
                                coluna("Situação", 1.2f), coluna("Motivo", 2f), coluna("Admissão", 1.3f),
                                coluna("Alta", 1.3f), coluna("Saída", 1.3f), coluna("Registrada por", 1.5f)),
                                linhas)),
                nomes.emissor(autenticacao),
                LocalDateTime.now(clock),
                true);
    }
}
