package com.br.octopus_msusuario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mensagem para o e-mail cadastrado do tutor. No texto, linhas com "## " viram título, linhas com "- " viram item
 * de lista e as demais, parágrafo; o e-mail sai em texto puro e em HTML.
 */
public record MensagemAoTutorRequest(
        @NotBlank @Size(max = 200) String assunto,
        @NotBlank @Size(max = 20000) String texto
) {
}
