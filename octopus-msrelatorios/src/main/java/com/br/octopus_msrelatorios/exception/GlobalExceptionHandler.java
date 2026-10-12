package com.br.octopus_msrelatorios.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Parâmetro da URL com o tipo errado: data fora do padrão aaaa-MM-dd, formato ou status que não existe.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' inválido.", req, null);
    }

    @ExceptionHandler(FiltroInvalidoException.class)
    public ResponseEntity<ErroResponse> handleFiltroInvalido(FiltroInvalidoException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ErroResponse> handleServicoIndisponivel(ServicoIndisponivelException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> handleAutenticacao(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Não autenticado", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAcessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Acesso negado", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex, HttpServletRequest req) {
        log.error("Erro não tratado em {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", req, null);
    }

    private ResponseEntity<ErroResponse> build(HttpStatus status, String mensagem, HttpServletRequest req, Map<String, String> campos) {
        ErroResponse body = campos == null
                ? ErroResponse.of(status.value(), status.getReasonPhrase(), mensagem, req.getRequestURI())
                : ErroResponse.comCampos(status.value(), status.getReasonPhrase(), mensagem, req.getRequestURI(), campos);
        return ResponseEntity.status(status).body(body);
    }
}
