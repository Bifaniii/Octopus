package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.relatorio.Relatorio;
import com.br.octopus_msrelatorios.relatorio.Valores;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.br.octopus_msrelatorios.relatorio.Relatorio.coluna;

/**
 * O que a clínica conta ao tutor quando o animal recebe alta: o atendimento, quem atendeu, as medicações e os
 * cuidados dos próximos 7 dias. É montado uma vez e vira o corpo do e-mail ({@link #paraTexto()}) ou, quando o
 * tutor não tem e-mail, o PDF que a recepção imprime ({@link #paraRelatorio}).
 */
record ResumoAlta(
        Long internacaoId,
        String animal,
        String especie,
        String tutor,
        String tutorEmail,
        Long tutorId,
        String motivo,
        LocalDateTime admissao,
        LocalDateTime alta,
        String tipoDaAlta,
        List<Pessoa> equipe,
        List<Medicacao> medicacoes,
        List<String> observacoes,
        List<Secao> cuidados
) {

    record Pessoa(String nome, String funcao, String atuacao) {
    }

    record Medicacao(String nome, String dosagem, String frequencia, long aplicadas, long naoAplicadas,
                     LocalDateTime primeira, LocalDateTime ultima, boolean prescricaoInterrompida) {

        String descricao() {
            StringBuilder s = new StringBuilder(nome).append(", ").append(dosagem).append(", ").append(frequencia)
                    .append(": ").append(aplicadas).append(aplicadas == 1 ? " dose aplicada" : " doses aplicadas");
            if (primeira != null) {
                s.append(" (de ").append(Valores.texto(primeira)).append(" a ").append(Valores.texto(ultima)).append(")");
            }
            if (naoAplicadas > 0) {
                s.append("; ").append(naoAplicadas).append(naoAplicadas == 1 ? " dose não foi aplicada no horário"
                        : " doses não foram aplicadas no horário");
            }
            if (prescricaoInterrompida) {
                s.append("; tratamento interrompido pelo veterinário");
            }
            return s.toString();
        }
    }

    record Secao(String titulo, List<String> itens) {
    }

    String assunto() {
        return "Resumo de alta de " + animal + " - Plantão VidaPet";
    }

    /** Texto do e-mail. O msusuario entende "## " como título e "- " como item de lista. */
    String paraTexto() {
        StringBuilder t = new StringBuilder();
        t.append("Olá, ").append(tutor).append(".\n\n");
        t.append(animal).append(" recebeu alta em ").append(Valores.texto(alta)).append(". Abaixo está o resumo do que foi "
                + "feito durante a internação e os cuidados recomendados para os próximos 7 dias.\n\n");

        t.append("## Atendimento\n");
        t.append("- Animal: ").append(animal).append(especie == null || especie.isBlank() ? "" : " (" + especie + ")").append('\n');
        t.append("- Motivo da internação: ").append(motivo).append('\n');
        t.append("- Admitido em: ").append(Valores.texto(admissao)).append('\n');
        t.append("- Alta em: ").append(Valores.texto(alta)).append(" - ").append(tipoDaAlta).append("\n\n");

        t.append("## Quem atendeu\n");
        equipe.forEach(p -> t.append("- ").append(p.nome()).append(" (").append(p.funcao()).append("): ")
                .append(p.atuacao()).append('\n'));
        t.append('\n');

        t.append("## Medicações\n");
        if (medicacoes.isEmpty()) {
            t.append("Nenhuma medicação foi aplicada durante a internação.\n");
        } else {
            medicacoes.forEach(m -> t.append("- ").append(m.descricao()).append('\n'));
        }
        t.append('\n');
        if (!observacoes.isEmpty()) {
            t.append("## Observações do veterinário\n");
            observacoes.forEach(o -> t.append("- ").append(o).append('\n'));
            t.append('\n');
        }

        t.append("## Cuidados para os próximos 7 dias\n");
        t.append("Orientações gerais. Elas não substituem o que o veterinário falar na alta: em caso de conflito, "
                + "siga o veterinário.\n");
        for (Secao secao : cuidados) {
            t.append("\n").append("## ").append(secao.titulo()).append('\n');
            secao.itens().forEach(i -> t.append("- ").append(i).append('\n'));
        }
        t.append("\nQualquer dúvida, fale com a clínica VidaPet. Desejamos uma ótima recuperação para ")
                .append(animal).append(".\n");
        return t.toString();
    }

    /** O mesmo conteúdo como PDF, para a recepção entregar ao tutor sem e-mail. */
    Relatorio paraRelatorio(String emitidoPor, LocalDateTime emitidoEm) {
        List<Relatorio.Tabela> tabelas = new ArrayList<>();
        List<List<Object>> atendimento = new ArrayList<>();
        atendimento.add(List.of("Animal", animal + (especie == null || especie.isBlank() ? "" : " (" + especie + ")")));
        atendimento.add(List.of("Tutor", tutor));
        atendimento.add(List.of("Motivo da internação", motivo));
        atendimento.add(List.of("Admitido em", admissao));
        atendimento.add(List.of("Alta em", alta));
        atendimento.add(List.of("Situação da alta", tipoDaAlta));
        tabelas.add(new Relatorio.Tabela("Atendimento", List.of(coluna("Item", 1.3f), coluna("Informação", 3f)), atendimento));

        List<List<Object>> pessoas = new ArrayList<>();
        equipe.forEach(p -> pessoas.add(List.of(p.nome(), p.funcao(), p.atuacao())));
        tabelas.add(new Relatorio.Tabela("Quem atendeu",
                List.of(coluna("Nome", 1.6f), coluna("Função", 1.1f), coluna("Atuação", 3f)), pessoas));

        List<List<Object>> remedios = new ArrayList<>();
        medicacoes.forEach(m -> remedios.add(List.of(m.nome(), m.dosagem(), m.frequencia(), m.aplicadas(),
                m.naoAplicadas(), m.prescricaoInterrompida() ? "Interrompido" : "Concluído ou em curso")));
        tabelas.add(new Relatorio.Tabela("Medicações", List.of(coluna("Medicamento", 2f), coluna("Dosagem", 1.2f),
                coluna("Frequência", 1.6f), coluna("Aplicadas", 0.9f), coluna("Não aplicadas", 1f),
                coluna("Situação", 1.5f)), remedios));

        if (!observacoes.isEmpty()) {
            tabelas.add(new Relatorio.Tabela("Observações do veterinário", List.of(coluna("Observação", 1f)),
                    observacoes.stream().<List<Object>>map(o -> List.of(o)).toList()));
        }
        for (Secao secao : cuidados) {
            tabelas.add(new Relatorio.Tabela("Cuidados: " + secao.titulo(), List.of(coluna("Orientação", 1f)),
                    secao.itens().stream().<List<Object>>map(i -> List.of(i)).toList()));
        }
        return new Relatorio("Resumo de alta", "resumo-de-alta-" + internacaoId,
                List.of("Cuidados gerais para os próximos 7 dias: não substituem a orientação do veterinário."),
                tabelas, emitidoPor, emitidoEm, false);
    }
}
