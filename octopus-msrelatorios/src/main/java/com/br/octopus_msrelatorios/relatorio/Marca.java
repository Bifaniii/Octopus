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

    /** Logo do sistema (pata sobre quadrado azul arredondado), 256 px, igual à do front: resources/logo/logo-app.png. */
    private static final byte[] LOGO_PNG = carregarLogo();

    static byte[] logoPng() {
        return LOGO_PNG;
    }

    private static byte[] carregarLogo() {
        try (java.io.InputStream entrada = Marca.class.getResourceAsStream("/logo/logo-app.png")) {
            if (entrada == null) {
                throw new java.io.UncheckedIOException(new java.io.FileNotFoundException("logo/logo-app.png"));
            }
            return entrada.readAllBytes();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private Marca() {
    }
}
