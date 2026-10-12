package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.client.UsuarioClient;
import com.br.octopus_msrelatorios.client.dto.TutorDto;
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
import java.util.List;
import java.util.Map;

import static com.br.octopus_msrelatorios.relatorio.Relatorio.coluna;

/** Cadastro de tutores e animais, com a mãe de cada animal e a situação da antirrábica (RN-02). */
@Service
@RequiredArgsConstructor
public class RelatorioTutoresAnimaisService {

    // Mesma regra do ms-internacao: vencida quando passou de 12 meses ou não tem registro.
    private static final int MESES_VALIDADE_ANTIRRABICA = 12;

    private final UsuarioClient usuarioClient;
    private final Pessoas pessoas;
    private final Clock clock;

    public Relatorio gerar(Authentication autenticacao) {
        Pessoas.Nomes nomes = pessoas.carregar();
        LocalDate hoje = LocalDate.now(clock);
        List<TutorDto> tutores = usuarioClient.tutores().stream()
                .sorted(Comparator.comparing(TutorDto::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<Long, String> nomeDoAnimal = new HashMap<>();
        tutores.forEach(t -> animais(t).forEach(a -> nomeDoAnimal.put(a.id(), a.nome())));

        List<List<Object>> linhasTutores = new ArrayList<>();
        List<List<Object>> linhasAnimais = new ArrayList<>();
        int vencidas = 0;
        for (TutorDto tutor : tutores) {
            linhasTutores.add(Arrays.asList(tutor.nome(), tutor.dataNascimento(), tutor.telefone(), tutor.endereco(),
                    (long) animais(tutor).size()));
            List<TutorDto.AnimalDto> ordenados = animais(tutor).stream()
                    .sorted(Comparator.comparing(TutorDto.AnimalDto::nome, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (TutorDto.AnimalDto animal : ordenados) {
                String vacina = situacaoAntirrabica(animal.dataUltimaAntirrabica(), hoje);
                if (!"Em dia".equals(vacina)) {
                    vencidas++;
                }
                linhasAnimais.add(Arrays.asList(animal.nome(), animal.especie(), tutor.nome(),
                        animal.maeId() == null ? null : nomeDoAnimal.getOrDefault(animal.maeId(), "Animal " + animal.maeId()),
                        animal.dataUltimaAntirrabica(), vacina));
            }
        }

        return new Relatorio(
                "Relatório de tutores e animais",
                "relatorio-tutores-animais",
                List.of(tutores.size() + " tutores, " + linhasAnimais.size() + " animais",
                        vencidas + " com antirrábica vencida ou sem registro (só podem ir para isolamento)"),
                List.of(
                        new Relatorio.Tabela("Tutores", List.of(
                                coluna("Tutor", 2), coluna("Nascimento", 1), coluna("Telefone", 1.2f),
                                coluna("Endereço", 3), coluna("Animais", 0.8f)), linhasTutores),
                        new Relatorio.Tabela("Animais", List.of(
                                coluna("Animal", 1.4f), coluna("Espécie", 1), coluna("Tutor", 2), coluna("Mãe", 1.4f),
                                coluna("Última antirrábica", 1.3f), coluna("Antirrábica", 1.4f)), linhasAnimais)),
                nomes.emissor(autenticacao),
                LocalDateTime.now(clock),
                false);
    }

    static String situacaoAntirrabica(LocalDate data, LocalDate hoje) {
        if (data == null) {
            return "Sem registro";
        }
        return data.plusMonths(MESES_VALIDADE_ANTIRRABICA).isBefore(hoje) ? "Vencida" : "Em dia";
    }

    private static List<TutorDto.AnimalDto> animais(TutorDto tutor) {
        return tutor.animais() == null ? List.of() : tutor.animais();
    }
}
