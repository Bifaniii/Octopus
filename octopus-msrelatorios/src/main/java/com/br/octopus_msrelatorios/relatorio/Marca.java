package com.br.octopus_msrelatorios.relatorio;

import java.awt.Color;

/**
 * Identidade visual dos relatórios, a mesma do front (src/styles/tema.css e o ícone da sidebar).
 * A logo é a pata: um círculo maior e quatro menores, desenhados num quadrado de 24 x 24.
 */
final class Marca {

    static final String NOME = "VidaPet";
    static final String SUBTITULO = "Plantão veterinário · Sistema Octopus";

    static final Color PRIMARIA = new Color(0x25, 0x63, 0xeb);
    static final Color PRIMARIA_FORTE = new Color(0x1e, 0x40, 0xaf);
    static final Color TEXTO = new Color(0x0f, 0x17, 0x2a);
    static final Color TEXTO_SUAVE = new Color(0x64, 0x74, 0x8b);
    static final Color BORDA = new Color(0xe2, 0xe8, 0xf0);
    static final Color FUNDO_ALTERNADO = new Color(0xf8, 0xfa, 0xfc);

    /** Cada linha: x, y (y para baixo, como no SVG) e raio. */
    static final float[][] PATA = {
            {12f, 14.5f, 4.2f},
            {5f, 7.5f, 2.1f},
            {10.2f, 4.3f, 2.1f},
            {14.8f, 4.3f, 2.1f},
            {19f, 7.5f, 2.1f},
    };

    private Marca() {
    }
}
