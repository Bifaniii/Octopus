package com.br.octopus_msusuario.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    public void enviarEmailRedefinicaoSenha(String destinatario, String token) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Recuperação de senha - VidaPet");
        mensagem.setText("""
                Você solicitou a redefinição de senha.

                Use o código abaixo para definir uma nova senha (válido por 30 minutos):
 
                %s
            
                Se você não solicitou isso, apenas ignore esse e-mail.
            """.formatted(token));
        mailSender.send(mensagem);
    }
}
