package com.br.octopus_msusuario.security;

import com.br.octopus_msusuario.domain.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SEGREDO = "dGVzdGUtc2VncmVkby1qd3QtYXBlbmFzLXBhcmEtdGVzdGVzLWF1dG9tYXRpemFkb3MtMDEyMzQ1Njc4OQ==";
    private static final String OUTRO_SEGREDO = "b3V0cm8tc2VncmVkby1jb20tNDgtYnl0ZXMtb3UtbWFpcy1wYXJhLWFzc2luYXItdG9rZW5zLTEyMw==";

    @Test
    void gerarToken_deveSerLidoDeVoltaComOEmail() {
        var jwtService = new JwtService(SEGREDO, 60_000);

        JwtService.TokenGerado gerado = jwtService.gerarToken("admin@vidapet.com", Role.ROLE_ADMIN);

        assertThat(jwtService.extrairEmail(gerado.token())).contains("admin@vidapet.com");
        assertThat(gerado.expiraEm()).isBetween(Instant.now().plusSeconds(55), Instant.now().plusSeconds(61));
    }

    @Test
    void extrairEmail_deveSerVazioParaTokenExpirado() {
        var jwtService = new JwtService(SEGREDO, -1_000);

        String token = jwtService.gerarToken("admin@vidapet.com", Role.ROLE_ADMIN).token();

        assertThat(jwtService.extrairEmail(token)).isEmpty();
    }

    @Test
    void extrairEmail_deveSerVazioParaTokenDeOutroSegredo() {
        String token = new JwtService(OUTRO_SEGREDO, 60_000).gerarToken("admin@vidapet.com", Role.ROLE_ADMIN).token();

        assertThat(new JwtService(SEGREDO, 60_000).extrairEmail(token)).isEmpty();
    }

    @Test
    void extrairEmail_deveSerVazioParaLixo() {
        assertThat(new JwtService(SEGREDO, 60_000).extrairEmail("nao-e-um-jwt")).isEmpty();
    }
}
