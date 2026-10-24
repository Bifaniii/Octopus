package com.br.octopus_msrelatorios.relatorio;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conteúdo de um relatório, sem saber se vai virar PDF ou planilha. Cada {@link Tabela} vira uma seção do PDF e
 * uma aba do XLSX. Os valores das linhas podem ser String, Number, LocalDate ou LocalDateTime: a planilha grava
 * datas e números com o tipo certo, e o PDF formata no padrão brasileiro.
 */
public record Relatorio(
        String titulo,
        String nomeArquivo,
        List<String> filtros,
        List<Tabela> tabelas,
        String emitidoPor,
        LocalDateTime emitidoEm,
        boolean paisagem,
        Assinatura assinatura
) {

    /** Os relatórios comuns não têm assinatura. */
    public Relatorio(String titulo, String nomeArquivo, List<String> filtros, List<Tabela> tabelas,
                     String emitidoPor, LocalDateTime emitidoEm, boolean paisagem) {
        this(titulo, nomeArquivo, filtros, tabelas, emitidoPor, emitidoEm, paisagem, null);
    }

    /** Bloco no fim do PDF: quem assina e quando. A planilha ignora. */
    public record Assinatura(String nome, LocalDateTime em) {
    }

    public record Tabela(String titulo, List<Coluna> colunas, List<List<Object>> linhas) {
    }

    /** {@code peso} é a largura relativa da coluna no PDF. */
    public record Coluna(String titulo, float peso) {
    }

    public static Coluna coluna(String titulo, float peso) {
        return new Coluna(titulo, peso);
    }
}
