package com.br.octopus_msusuario.mail;

/** Converte o texto da mensagem ao tutor (marcações "## " e "- ") em HTML e em texto puro. */
final class MensagemFormatada {

    private MensagemFormatada() {
    }

    static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String textoPuro(String texto) {
        StringBuilder saida = new StringBuilder();
        for (String linha : texto.strip().split("\\R", -1)) {
            saida.append(linha.startsWith("## ") ? linha.substring(3).toUpperCase(java.util.Locale.ROOT) : linha).append('\n');
        }
        return saida.toString();
    }

    static String html(String texto) {
        StringBuilder html = new StringBuilder();
        boolean emLista = false;
        for (String linha : texto.strip().split("\\R")) {
            boolean item = linha.startsWith("- ");
            if (emLista && !item) {
                html.append("</ul>");
                emLista = false;
            }
            if (linha.isBlank()) {
                continue;
            }
            if (linha.startsWith("## ")) {
                html.append("<h3 style=\"margin:22px 0 8px;font-size:16px;color:#1e40af;\">")
                        .append(escapar(linha.substring(3))).append("</h3>");
            } else if (item) {
                if (!emLista) {
                    html.append("<ul style=\"margin:0 0 12px;padding-left:20px;\">");
                    emLista = true;
                }
                html.append("<li style=\"margin:0 0 4px;\">").append(escapar(linha.substring(2))).append("</li>");
            } else {
                html.append("<p style=\"margin:0 0 12px;\">").append(escapar(linha)).append("</p>");
            }
        }
        if (emLista) {
            html.append("</ul>");
        }
        return html.toString();
    }
}
