package com.br.octopus_msinternacao.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// O resumo de alta (e-mail ao tutor) é montado no octopus-msrelatorios. Aqui só se pede o envio, sem derrubar a alta.
@Component
public class RelatoriosClient {

    private static final Logger log = LoggerFactory.getLogger(RelatoriosClient.class);

    private final RestClient restClient;

    public RelatoriosClient(@Value("${app.servicos.relatorios-url}") String baseUrl) {
        // O msrelatorios lê quatro módulos e ainda fala com o SMTP: os 3 s dos outros clients são pouco.
        this.restClient = ClientesHttp.criar(baseUrl, java.time.Duration.ofSeconds(20));
    }

    /**
     * Pede o envio do resumo de alta ao e-mail do tutor. O token é o de quem registrou a alta, já que esta chamada roda
     * fora da requisição. Qualquer falha (tutor sem e-mail, módulo fora do ar) só é registrada: a alta já foi gravada.
     */
    public void enviarResumoDeAlta(Long internacaoId, String token) {
        try {
            restClient.post()
                    .uri("/api/relatorios/altas/{id}/email", internacaoId)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Resumo de alta da internação {} enviado ao tutor.", internacaoId);
        } catch (Exception e) {
            log.warn("Resumo de alta da internação {} não foi enviado: {}", internacaoId, e.getMessage());
        }
    }
}
