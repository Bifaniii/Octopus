package com.br.octopus_msbaias.exception;

import java.time.Instant;
import java.util.Map;

public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String path,
        Map<String, String> campos
) {
    public static ErroResponse of(int status, String erro, String mensagem, String path) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, path, null);
    }

    public static ErroResponse comCampos(int status, String erro, String mensagem, String path, Map<String, String> campos) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, path, campos);
    }
}
