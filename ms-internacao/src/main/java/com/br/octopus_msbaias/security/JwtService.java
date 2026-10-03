package com.br.octopus_msbaias.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Optional;

// Este módulo NÃO emite tokens: ele só valida o JWT emitido pelo octopus-msusuario, usando o mesmo
// JWT_SECRET (HS384). Sem tabela de usuários aqui — a identidade/role vêm direto dos claims do token.
@Service
public class JwtService {

    private final SecretKey chave;

    public JwtService(@Value("${app.security.jwt.secret}") String secretBase64) {
        this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    public record DadosToken(String email, String role) {
    }

    public Optional<DadosToken> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            if (email == null || role == null) {
                return Optional.empty();
            }
            return Optional.of(new DadosToken(email, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
