package com.br.octopus_msusuario.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Os dois e-mails saem com a logo embutida (cid:logo) e em texto puro mais HTML. */
class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailService emailService = new EmailService(mailSender);

    private String enviar(Runnable envio) throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        ReflectionTestUtils.setField(emailService, "remetente", "clinica@vidapet.local");
        envio.run();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensagem = captor.getValue();
        mensagem.saveChanges();
        ByteArrayOutputStream bruto = new ByteArrayOutputStream();
        mensagem.writeTo(bruto);
        return bruto.toString(StandardCharsets.ISO_8859_1);
    }

    @Test
    void emailDeRedefinicaoDeSenhaLevaALogoEmbutida() throws Exception {
        String bruto = enviar(() -> emailService.enviarEmailRedefinicaoSenha("a@b.com", "123456", 15));

        assertThat(bruto).contains("multipart/related", "Content-ID: <logo>", "Content-Type: image/png", "cid:logo");
    }

    @Test
    void emailAoTutorLevaALogoEmbutida() throws Exception {
        String bruto = enviar(() -> emailService.enviarMensagemAoTutor("t@b.com", "Resumo de alta", "## Título\n- item"));

        assertThat(bruto).contains("multipart/related", "Content-ID: <logo>", "Content-Type: image/png", "cid:logo",
                "text/plain", "text/html");
    }
}
