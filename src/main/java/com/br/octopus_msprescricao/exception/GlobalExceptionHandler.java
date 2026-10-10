package com.br.octopus_msprescricao.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(), error.getDefaultMessage());
        }

        // Usando o método build existente para garantir que o mapa de campos seja populado corretamente
        return build(HttpStatus.BAD_REQUEST, "Erro de validação nos campos da requisição.", request, campos);
    }

    /**
     * Corpo que o Jackson não consegue ler: JSON malformado, data/hora fora do padrão ISO ou valor que não
     * existe no enum. É erro de quem chamou, não do servidor, então 400 e não 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Datas e horas usam o formato aaaa-MM-ddTHH:mm:ss.", req, null);
    }

    // Parâmetro da URL com o tipo errado, por exemplo um id que não é número.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' inválido.", req, null);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(RegraDeNegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, null);
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
