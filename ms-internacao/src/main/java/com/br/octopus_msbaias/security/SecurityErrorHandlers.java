package com.br.octopus_msbaias.security;

import com.br.octopus_msbaias.exception.ErroResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Exceções lançadas dentro da filter chain não chegam ao @RestControllerAdvice, então o JSON de erro é montado aqui.
@Component
@RequiredArgsConstructor
public class SecurityErrorHandlers {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint entryPoint() {
        return (req, res, ex) -> escrever(req, res, HttpStatus.UNAUTHORIZED, "Não autenticado");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (req, res, ex) -> escrever(req, res, HttpStatus.FORBIDDEN, "Acesso negado");
    }

    private void escrever(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String mensagem) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        ErroResponse body = ErroResponse.of(status.value(), status.getReasonPhrase(), mensagem, req.getRequestURI());
        objectMapper.writeValue(res.getWriter(), body);
    }
}
