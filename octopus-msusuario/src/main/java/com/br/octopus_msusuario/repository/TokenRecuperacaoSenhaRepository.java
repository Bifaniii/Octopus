package com.br.octopus_msusuario.repository;

import com.br.octopus_msusuario.domain.TokenRecuperacaoSenha;
import com.br.octopus_msusuario.domain.Usuario;
import com.br.octopus_msusuario.domain.enums.StatusTokenSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, UUID> {

    Optional<TokenRecuperacaoSenha> findByTokenAndStatus(String token, StatusTokenSenha status);

    List<TokenRecuperacaoSenha> findByUsuarioAndStatus(Usuario usuario, StatusTokenSenha status);
}
