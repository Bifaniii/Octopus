package com.br.octopus_msmedications.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Optional;

// Só valida o token emitido pelo octopus-msusuario, com o mesmo JWT_SECRET. Não emite token e não
// consulta usuários: identidade e papel saem dos claims.
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
