package com.br.octopus_msprescricao.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Optional;

/**
 * Só valida o token. Quem emite é o octopus-msusuario, com o mesmo JWT_SECRET (HS384).
 * Este módulo não tem tabela de usuários: identidade e papel vêm dos claims,
 * onde "sub" é o e-mail e "role" é a ROLE_* (ex.: ROLE_VETERINARIO).
 */
@Service
public class JwtService {

    private final SecretKey chave;

    public JwtService(@Value("${app.security.jwt.secret}") String secretBase64) {
        this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    public record DadosToken(String email, String role) {
    }

    /** Vazio se a assinatura não bate, o token expirou, está malformado ou não traz e-mail e role. */
    public Optional<DadosToken> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            if (email == null || email.isBlank() || role == null || role.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new DadosToken(email, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
