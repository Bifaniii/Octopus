package com.br.octopus_msusuario.security;

import com.br.octopus_msusuario.domain.Usuario;
import com.br.octopus_msusuario.domain.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UsuarioDetails implements UserDetails {

    private final Usuario usuario;

    public UsuarioDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    public Role getRole() {
        return usuario.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(usuario.getRole().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getSenha();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return usuario.isAtivo();
    }
}
