package com.br.octopus_msprescricao.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String OUTRO_SEGREDO = "b3V0cm8tc2VncmVkby1jb20tNDgtYnl0ZXMtb3UtbWFpcy1wYXJhLWFzc2luYXItdG9rZW5zLTEyMw==";

    private final JwtService jwtService = new JwtService(TokensDeTeste.SEGREDO);

    @Test
    void validar_deveDevolverEmailERoleDoToken() {
        String token = TokensDeTeste.gerar("vet@vidapet.com", "ROLE_VETERINARIO");

        assertThat(jwtService.validar(token))
                .contains(new JwtService.DadosToken("vet@vidapet.com", "ROLE_VETERINARIO"));
    }

    @Test
    void validar_deveSerVazioParaTokenExpirado() {
        String token = TokensDeTeste.gerar("vet@vidapet.com", "ROLE_VETERINARIO", TokensDeTeste.SEGREDO, -1_000);

        assertThat(jwtService.validar(token)).isEmpty();
    }

    @Test
    void validar_deveSerVazioParaTokenDeOutroSegredo() {
        String token = TokensDeTeste.gerar("vet@vidapet.com", "ROLE_VETERINARIO", OUTRO_SEGREDO, 60_000);

        assertThat(jwtService.validar(token)).isEmpty();
    }

    @Test
    void validar_deveSerVazioParaTokenSemRole() {
        assertThat(jwtService.validar(TokensDeTeste.semRole("vet@vidapet.com"))).isEmpty();
    }

    @Test
    void validar_deveSerVazioParaLixo() {
        assertThat(jwtService.validar("nao-e-um-jwt")).isEmpty();
    }
}
