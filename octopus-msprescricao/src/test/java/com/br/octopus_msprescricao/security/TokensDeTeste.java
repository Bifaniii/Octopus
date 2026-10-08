package com.br.octopus_msprescricao.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.time.Instant;
import java.util.Date;

/**
 * Emite tokens como o octopus-msusuario faria (sub = e-mail, claim "role"), porque este módulo só valida.
 * O segredo é o mesmo de src/test/resources/application.properties.
 */
public final class TokensDeTeste {

    public static final String SEGREDO = "dGVzdGUtc2VncmVkby1qd3QtYXBlbmFzLXBhcmEtdGVzdGVzLWF1dG9tYXRpemFkb3MtMDEyMzQ1Njc4OQ==";

    private TokensDeTeste() {
    }

    public static String gerar(String email, String role) {
        return gerar(email, role, SEGREDO, 60_000);
    }

    public static String gerar(String email, String role, String segredoBase64, long expiraEmMs) {
        var chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredoBase64));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusMillis(expiraEmMs)))
                .signWith(chave)
                .compact();
    }

    /** Token assinado e válido, mas sem o claim "role". */
    public static String semRole(String email) {
        var chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SEGREDO));
        return Jwts.builder()
                .subject(email)
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(chave)
                .compact();
    }

    public static String bearer(String email, String role) {
        return "Bearer " + gerar(email, role);
    }
}
