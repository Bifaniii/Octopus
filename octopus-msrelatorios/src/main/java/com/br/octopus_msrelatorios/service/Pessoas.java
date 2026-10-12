package com.br.octopus_msrelatorios.service;

import com.br.octopus_msrelatorios.client.UsuarioClient;
import com.br.octopus_msrelatorios.client.dto.UsuarioNomeDto;
import com.br.octopus_msrelatorios.domain.Rotulos;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Os outros módulos gravam só o e-mail de quem fez cada operação; o relatório mostra o nome. A lista vem do
 * msusuario uma vez por relatório.
 */
@Component
@RequiredArgsConstructor
public class Pessoas {

    private final UsuarioClient usuarioClient;

    public Nomes carregar() {
        List<UsuarioNomeDto> lista = usuarioClient.nomes();
        Map<String, String> porEmail = new HashMap<>();
        lista.forEach(u -> porEmail.put(u.email().toLowerCase(Locale.ROOT), u.nome()));
        return new Nomes(porEmail);
    }

    public record Nomes(Map<String, String> porEmail) {

        /** Nome da pessoa; se o e-mail não for de nenhuma conta, o próprio e-mail. */
        public String de(String email) {
            if (email == null || email.isBlank()) {
                return null;
            }
            return porEmail.getOrDefault(email.toLowerCase(Locale.ROOT), email);
        }

        /** "Nome (Perfil)" de quem pediu o relatório, para o rodapé. */
        public String emissor(Authentication autenticacao) {
            String role = autenticacao.getAuthorities().iterator().next().getAuthority();
            return de(autenticacao.getName()) + " (" + Rotulos.perfil(role) + ")";
        }
    }
}
