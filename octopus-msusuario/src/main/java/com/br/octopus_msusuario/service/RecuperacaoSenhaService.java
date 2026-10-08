package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.TokenRecuperacaoSenha;
import com.br.octopus_msusuario.domain.Usuario;
import com.br.octopus_msusuario.domain.enums.StatusTokenSenha;
import com.br.octopus_msusuario.dto.request.EsqueciSenhaRequest;
import com.br.octopus_msusuario.dto.request.RedefinirSenhaRequest;
import com.br.octopus_msusuario.exception.TokenInvalidoException;
import com.br.octopus_msusuario.mail.EmailService;
import com.br.octopus_msusuario.repository.TokenRecuperacaoSenhaRepository;
import com.br.octopus_msusuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecuperacaoSenhaService {

    // Vai junto para o e-mail, então o prazo prometido no texto nunca diverge do prazo real.
    private static final int VALIDADE_MINUTOS = 30;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Sempre termina em silêncio, exista o e-mail ou não. Responder diferente para e-mail inexistente
     * entregaria a quem perguntasse a lista de quem tem conta na clínica.
     */
    @Transactional
    public void solicitar(EsqueciSenhaRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(request.email());
        if (encontrado.isEmpty() || !encontrado.get().isAtivo()) {
            log.info("Pedido de redefinição para e-mail sem conta ativa; nada foi enviado.");
            return;
        }
        Usuario usuario = encontrado.get();

        arquivarTokensAtivos(usuario);
        
        String codigoNumero = String.format("%06d", new java.util.Random().nextInt(1000000));

        TokenRecuperacaoSenha novo = TokenRecuperacaoSenha.builder()
                .usuario(usuario)
                .token(codigoNumero)
                .expiraEm(LocalDateTime.now().plusMinutes(VALIDADE_MINUTOS))
                .build();
        tokenRepository.save(novo);

        // Um SMTP fora do ar não pode virar erro 500 nem desfazer o token já gravado: o usuário pede de novo.
        try {
            emailService.enviarEmailRedefinicaoSenha(usuario.getEmail(), novo.getToken(), VALIDADE_MINUTOS);
        } catch (Exception e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            log.warn("Não foi possível enviar o e-mail de redefinição de senha: {}", causa.getMessage());
        }
    }

    @Transactional
    public void redefinir(RedefinirSenhaRequest request) {
        TokenRecuperacaoSenha token = tokenRepository
                .findByTokenAndStatus(request.token(), StatusTokenSenha.ATIVO)
                .orElseThrow(() -> new TokenInvalidoException("Código inválido ou já utilizado."));

        if (token.expirado()) {
            token.setStatus(StatusTokenSenha.ARQUIVADO);
            throw new TokenInvalidoException("Código expirado. Peça um novo.");
        }

        Usuario usuario = token.getUsuario();
        if (!usuario.isAtivo()) {
            throw new TokenInvalidoException("Código inválido ou já utilizado.");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        token.setStatus(StatusTokenSenha.USADO);
        token.setUsadoEm(LocalDateTime.now());
    }

    /** Um pedido novo invalida o anterior: fica um código válido por vez, e o antigo vira histórico. */
    private void arquivarTokensAtivos(Usuario usuario) {
        tokenRepository.findByUsuarioAndStatus(usuario, StatusTokenSenha.ATIVO)
                .forEach(token -> token.setStatus(StatusTokenSenha.ARQUIVADO));
    }
}
