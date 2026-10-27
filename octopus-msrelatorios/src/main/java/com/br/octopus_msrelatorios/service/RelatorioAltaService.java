package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.client.DoseClient;
import com.br.octopus_msrelatorios.client.InternacaoClient;
import com.br.octopus_msrelatorios.client.MedicacaoClient;
import com.br.octopus_msrelatorios.client.PrescricaoClient;
import com.br.octopus_msrelatorios.client.UsuarioClient;
import com.br.octopus_msrelatorios.client.dto.DoseDto;
import com.br.octopus_msrelatorios.client.dto.InternacaoDto;
import com.br.octopus_msrelatorios.client.dto.InternacaoEventoDto;
import com.br.octopus_msrelatorios.client.dto.MedicacaoDto;
import com.br.octopus_msrelatorios.client.dto.PrescricaoDto;
import com.br.octopus_msrelatorios.client.dto.TutorDto;
import com.br.octopus_msrelatorios.exception.RecursoNaoEncontradoException;
import com.br.octopus_msrelatorios.exception.RegraDeNegocioException;
import com.br.octopus_msrelatorios.relatorio.Relatorio;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resumo de alta do animal: lido dos outros módulos (internação, prescrições, doses, tutor) quando a alta é
 * registrada, e entregue ao tutor por e-mail ou, sem e-mail cadastrado, em PDF.
 */
@Service
@RequiredArgsConstructor
public class RelatorioAltaService {

    private static final Set<String> COM_ALTA = Set.of("ALTA_AUTORIZADA", "ALTA_A_PEDIDO_DO_TUTOR", "ENCERRADA");

    private final InternacaoClient internacaoClient;
    private final PrescricaoClient prescricaoClient;
    private final DoseClient doseClient;
    private final MedicacaoClient medicacaoClient;
    private final UsuarioClient usuarioClient;
    private final Pessoas pessoas;
    private final Clock clock;

    public Relatorio pdf(Long internacaoId, Authentication autenticacao) {
        ResumoAlta resumo = montar(internacaoId);
        return resumo.paraRelatorio(pessoas.carregar().emissor(autenticacao), LocalDateTime.now(clock));
    }

    /** Resultado do envio: para quem o e-mail foi. */
    public record Envio(String destinatario) {
    }

    public Envio enviar(Long internacaoId) {
        ResumoAlta resumo = montar(internacaoId);
        if (resumo.tutorEmail() == null) {
            throw new RegraDeNegocioException("O tutor " + resumo.tutor()
                    + " não tem e-mail cadastrado. Baixe o resumo em PDF para entregar a ele.");
        }
        String destinatario = usuarioClient.enviarMensagemAoTutor(resumo.tutorId(), resumo.assunto(), resumo.paraTexto());
        return new Envio(destinatario);
    }

    ResumoAlta montar(Long internacaoId) {
        InternacaoDto internacao = internacaoClient.listar().stream()
                .filter(i -> i.id().equals(internacaoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Internação não encontrada: " + internacaoId));
        if (!COM_ALTA.contains(internacao.status())) {
            throw new RegraDeNegocioException("A internação " + internacaoId
                    + " ainda não teve alta, então não há resumo de alta.");
        }
        TutorDto tutor = usuarioClient.tutores().stream()
                .filter(t -> t.animais() != null && t.animais().stream().anyMatch(a -> a.id().equals(internacao.animalId())))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Tutor do animal " + internacao.animalNome() + " não encontrado."));
        List<InternacaoEventoDto> eventos = internacaoClient.eventos(internacaoId);
        List<PrescricaoDto> prescricoes = prescricaoClient.listarDaInternacao(internacaoId);
        List<DoseDto> doses = doseClient.listarDaInternacao(internacaoId);
        Map<Long, String> nomesDosRemedios = medicacaoClient.listar().stream()
                .collect(Collectors.toMap(MedicacaoDto::id, this::nomeDoMedicamento, (a, b) -> a));
        Pessoas.Nomes nomes = pessoas.carregar();

        InternacaoEventoDto eventoDaAlta = eventos.stream()
                .filter(e -> "ALTA_AUTORIZADA".equals(e.statusNovo()) || "ALTA_A_PEDIDO_DO_TUTOR".equals(e.statusNovo()))
                .findFirst().orElse(null);
        LocalDateTime alta = internacao.dataAlta() != null ? internacao.dataAlta()
                : eventoDaAlta != null ? eventoDaAlta.dataHora() : LocalDateTime.now(clock);
        String tipo = eventoDaAlta != null && "ALTA_A_PEDIDO_DO_TUTOR".equals(eventoDaAlta.statusNovo())
                || "ALTA_A_PEDIDO_DO_TUTOR".equals(internacao.status())
                ? "alta a pedido do tutor" : "alta autorizada pelo veterinário";

        return new ResumoAlta(internacaoId, internacao.animalNome(), internacao.animalEspecie(), tutor.nome(),
                tutor.email(), tutor.id(), internacao.motivo(), internacao.dataAdmissao(), alta, tipo,
                equipe(internacao, eventos, prescricoes, doses, nomes),
                medicacoes(prescricoes, doses, nomesDosRemedios),
                prescricoes.stream().map(PrescricaoDto::observacao)
                        .filter(o -> o != null && !o.isBlank()).distinct().toList(),
                cuidados(internacao.animalEspecie()));
    }

    private List<ResumoAlta.Pessoa> equipe(InternacaoDto internacao, List<InternacaoEventoDto> eventos,
                                           List<PrescricaoDto> prescricoes, List<DoseDto> doses, Pessoas.Nomes nomes) {
        // E-mail -> o que a pessoa fez, na ordem em que apareceu.
        Map<String, List<String>> atuacoes = new LinkedHashMap<>();
        atuacoes.computeIfAbsent(internacao.registradoPor(), k -> new ArrayList<>()).add("registrou a admissão");
        for (InternacaoEventoDto e : eventos) {
            String acao = switch (e.statusNovo()) {
                case "EM_TRATAMENTO" -> "iniciou o tratamento";
                case "ISOLAMENTO" -> "colocou em isolamento";
                case "ALTA_AUTORIZADA" -> "autorizou a alta";
                case "ALTA_A_PEDIDO_DO_TUTOR" -> "registrou a alta a pedido do tutor";
                case "ENCERRADA" -> "registrou a saída da clínica";
                default -> null;
            };
            if (acao != null && e.usuario() != null) {
                atuacoes.computeIfAbsent(e.usuario(), k -> new ArrayList<>()).add(acao);
            }
        }
        for (PrescricaoDto p : prescricoes) {
            atuacoes.computeIfAbsent(p.veterinarioEmail(), k -> new ArrayList<>())
                    .add("prescreveu " + p.itens().size() + (p.itens().size() == 1 ? " medicamento" : " medicamentos"));
        }
        Map<String, Long> aplicacoes = doses.stream()
                .filter(d -> "APLICADA".equals(d.status()) && d.aplicadaPor() != null)
                .collect(Collectors.groupingBy(DoseDto::aplicadaPor, LinkedHashMap::new, Collectors.counting()));
        aplicacoes.forEach((email, n) -> atuacoes.computeIfAbsent(email, k -> new ArrayList<>())
                .add("aplicou " + n + (n == 1 ? " dose" : " doses")));

        List<ResumoAlta.Pessoa> equipe = new ArrayList<>();
        atuacoes.forEach((email, lista) -> {
            if (email == null) {
                return;
            }
            String perfil = nomes.perfilDe(email);
            String nome = nomes.de(email);
            equipe.add(new ResumoAlta.Pessoa("Veterinário".equals(perfil) ? "Dr(a). " + semTratamento(nome) : nome,
                    perfil == null ? "Equipe da clínica" : perfil, String.join("; ", lista)));
        });
        return equipe;
    }

    private List<ResumoAlta.Medicacao> medicacoes(List<PrescricaoDto> prescricoes, List<DoseDto> doses,
                                                  Map<Long, String> nomes) {
        List<ResumoAlta.Medicacao> lista = new ArrayList<>();
        for (PrescricaoDto p : prescricoes) {
            for (PrescricaoDto.ItemDto item : p.itens()) {
                List<DoseDto> doItem = doses.stream()
                        .filter(d -> d.prescricaoId().equals(p.id()) && d.medicacaoId().equals(item.medicacaoId()))
                        .toList();
                List<LocalDateTime> aplicadas = doItem.stream().filter(d -> "APLICADA".equals(d.status()))
                        .map(DoseDto::aplicadaEm).filter(java.util.Objects::nonNull).sorted().toList();
                long naoAplicadas = doItem.stream().filter(d -> "PERDIDA".equals(d.status())).count();
                if (p.desativada() && aplicadas.isEmpty()) {
                    continue; // prescrição desativada à mão que nunca foi aplicada não entra no resumo
                }
                lista.add(new ResumoAlta.Medicacao(
                        nomes.getOrDefault(item.medicacaoId(), "Medicamento " + item.medicacaoId()),
                        item.dosagem(),
                        item.seNecessario() ? "se necessário (mínimo " + item.intervaloHoras() + " h entre doses)"
                                : "a cada " + item.intervaloHoras() + " h",
                        aplicadas.size(), naoAplicadas,
                        aplicadas.isEmpty() ? null : aplicadas.get(0),
                        aplicadas.isEmpty() ? null : aplicadas.get(aplicadas.size() - 1),
                        p.desativada()));
            }
        }
        lista.sort(Comparator.comparing(ResumoAlta.Medicacao::nome));
        return lista;
    }

    /** Cuidados padrão por espécie (cão, gato ou outros), lidos de resources/cuidados/*.txt. */
    List<ResumoAlta.Secao> cuidados(String especie) {
        String arquivo = switch (normalizar(especie)) {
            case "cao", "cachorro", "canino" -> "cao";
            case "gato", "felino" -> "gato";
            default -> "outros";
        };
        List<ResumoAlta.Secao> secoes = new ArrayList<>();
        String titulo = null;
        List<String> itens = new ArrayList<>();
        try {
            String texto = new ClassPathResource("cuidados/" + arquivo + ".txt").getContentAsString(StandardCharsets.UTF_8);
            for (String linha : texto.split("\\R")) {
                if (linha.startsWith("## ")) {
                    if (titulo != null) {
                        secoes.add(new ResumoAlta.Secao(titulo, itens));
                    }
                    titulo = linha.substring(3).trim();
                    itens = new ArrayList<>();
                } else if (linha.startsWith("- ")) {
                    itens.add(linha.substring(2).trim());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (titulo != null) {
            secoes.add(new ResumoAlta.Secao(titulo, itens));
        }
        return secoes;
    }

    private static String normalizar(String especie) {
        if (especie == null) {
            return "";
        }
        return Normalizer.normalize(especie, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    private String nomeDoMedicamento(MedicacaoDto m) {
        return m.concentracao() == null || m.concentracao().isBlank()
                ? m.nomeComercial() : m.nomeComercial() + " " + m.concentracao();
    }

    private static String semTratamento(String nome) {
        return nome == null ? "" : nome.replaceFirst("(?i)^\\s*dra?\\.?\\s+", "").trim();
    }
}
