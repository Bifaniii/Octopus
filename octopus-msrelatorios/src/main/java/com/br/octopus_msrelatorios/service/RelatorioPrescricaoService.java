package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.client.InternacaoClient;
import com.br.octopus_msrelatorios.client.MedicacaoClient;
import com.br.octopus_msrelatorios.client.PrescricaoClient;
import com.br.octopus_msrelatorios.client.UsuarioClient;
import com.br.octopus_msrelatorios.client.dto.InternacaoDto;
import com.br.octopus_msrelatorios.client.dto.MedicacaoDto;
import com.br.octopus_msrelatorios.client.dto.PrescricaoDto;
import com.br.octopus_msrelatorios.client.dto.TutorDto;
import com.br.octopus_msrelatorios.domain.Rotulos;
import com.br.octopus_msrelatorios.exception.RecursoNaoEncontradoException;
import com.br.octopus_msrelatorios.relatorio.Relatorio;
import com.br.octopus_msrelatorios.relatorio.Valores;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.br.octopus_msrelatorios.relatorio.Relatorio.coluna;

/**
 * Uma prescrição para imprimir: paciente, tutor, itens e a assinatura do veterinário que prescreveu. A assinatura
 * é um carimbo com o nome e o momento da emissão, não um certificado digital.
 */
@Service
@RequiredArgsConstructor
public class RelatorioPrescricaoService {

    private final PrescricaoClient prescricaoClient;
    private final InternacaoClient internacaoClient;
    private final MedicacaoClient medicacaoClient;
    private final UsuarioClient usuarioClient;
    private final Pessoas pessoas;
    private final Clock clock;

    public Relatorio gerar(Long prescricaoId, Authentication autenticacao) {
        PrescricaoDto prescricao = prescricaoClient.buscar(prescricaoId);
        InternacaoDto internacao = internacaoClient.listar().stream()
                .filter(i -> i.id().equals(prescricao.internacaoId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Internação da prescrição não encontrada: " + prescricao.internacaoId()));
        TutorDto tutor = usuarioClient.tutores().stream()
                .filter(t -> t.animais() != null && t.animais().stream().anyMatch(a -> a.id().equals(internacao.animalId())))
                .findFirst()
                .orElse(null);
        Map<Long, String> medicamentos = medicacaoClient.listar().stream()
                .collect(Collectors.toMap(MedicacaoDto::id, this::nomeDoMedicamento, (a, b) -> a));
        Pessoas.Nomes nomes = pessoas.carregar();

        String veterinario = comTratamento(nomes.de(prescricao.veterinarioEmail()));
        LocalDateTime agora = LocalDateTime.now(clock);

        List<List<Object>> itens = new ArrayList<>();
        for (PrescricaoDto.ItemDto item : prescricao.itens()) {
            itens.add(List.of(
                    medicamentos.getOrDefault(item.medicacaoId(), "Medicamento " + item.medicacaoId()),
                    item.dosagem(),
                    frequencia(item),
                    duracao(item),
                    item.inicio() == null ? "" : item.inicio(),
                    item.observacao() == null ? "" : item.observacao()));
        }

        List<Relatorio.Tabela> tabelas = new ArrayList<>();
        tabelas.add(new Relatorio.Tabela("Paciente e tutor", List.of(
                coluna("Animal", 1.4f), coluna("Espécie", 1f), coluna("Tutor", 2f), coluna("Telefone", 1.3f),
                coluna("Situação", 1.3f)),
                List.<List<Object>>of(List.of(
                        internacao.animalNome(),
                        internacao.animalEspecie() == null ? "" : internacao.animalEspecie(),
                        tutor == null ? "Não encontrado" : tutor.nome(),
                        tutor == null || tutor.telefone() == null ? "" : Telefones.formatar(tutor.telefone()),
                        Rotulos.statusInternacao(internacao.status())))));
        tabelas.add(new Relatorio.Tabela("Medicamentos prescritos", List.of(
                coluna("Medicamento", 2f), coluna("Dosagem", 1.3f), coluna("Frequência", 1.8f),
                coluna("Duração", 1.2f), coluna("Início", 1.4f), coluna("Observação", 1.8f)), itens));
        if (prescricao.observacao() != null && !prescricao.observacao().isBlank()) {
            tabelas.add(new Relatorio.Tabela("Observações do veterinário", List.of(coluna("Observação", 1f)),
                    List.<List<Object>>of(List.of(prescricao.observacao()))));
        }

        List<String> filtros = new ArrayList<>();
        filtros.add("Prescrição " + prescricao.id() + " · internação " + internacao.id());
        filtros.add("Prescrita em " + Valores.texto(prescricao.criadoEm()));
        if (prescricao.desativada()) {
            filtros.add("PRESCRIÇÃO DESATIVADA");
        } else if (prescricao.concluida()) {
            filtros.add("PRESCRIÇÃO CONCLUÍDA");
        }

        return new Relatorio(
                "Prescrição médica",
                "prescricao-" + prescricao.id(),
                filtros,
                tabelas,
                nomes.emissor(autenticacao),
                agora,
                false,
                new Relatorio.Assinatura(veterinario, agora));
    }

    private String nomeDoMedicamento(MedicacaoDto m) {
        return m.concentracao() == null || m.concentracao().isBlank()
                ? m.nomeComercial()
                : m.nomeComercial() + " " + m.concentracao();
    }

    private static String frequencia(PrescricaoDto.ItemDto item) {
        return item.seNecessario()
                ? "Se necessário (mínimo " + item.intervaloHoras() + " h entre doses)"
                : "A cada " + item.intervaloHoras() + " h";
    }

    private static String duracao(PrescricaoDto.ItemDto item) {
        if (item.seNecessario() || item.quantidadeDoses() == null) {
            return "Enquanto necessário";
        }
        return item.quantidadeDoses() + (item.quantidadeDoses() == 1 ? " dose" : " doses");
    }

    /** "Dr. Nome": tira o tratamento que o cadastro já tenha para não sair "Dr. Dr. Nome". */
    private static String comTratamento(String nome) {
        String limpo = nome == null ? "" : nome.replaceFirst("(?i)^\\s*dra?\\.?\\s+", "").trim();
        return "Dr. " + limpo;
    }

    /** Telefone só com dígitos (como o msusuario guarda) para (11) 99999-9999. */
    static final class Telefones {
        static String formatar(String telefone) {
            String d = telefone.replaceAll("\\D", "");
            return d.length() == 11 ? "(%s) %s-%s".formatted(d.substring(0, 2), d.substring(2, 7), d.substring(7)) : telefone;
        }
    }
}
