package com.br.octopus_msusuario.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class EmailService {

    private static final String NOME_REMETENTE = "Plantão VidaPet";
    private static final String VERDE = "#1f7f6b";
    private static final String TINTA = "#17242b";
    private static final String TINTA_SUAVE = "#5b6c74";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    /**
     * Envia o código em multipart: texto puro e HTML. Cliente que não renderiza HTML lê a versão em texto, e
     * mandar as duas reduz a chance de o filtro tratar a mensagem como spam.
     */
    public void enviarEmailRedefinicaoSenha(String destinatario, String token, int validadeMinutos) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente, NOME_REMETENTE);
            helper.setTo(destinatario);
            helper.setSubject("Seu código para redefinir a senha do Plantão");
            helper.setText(textoPuro(token, validadeMinutos), html(token, validadeMinutos));
            mailSender.send(mime);
        } catch (jakarta.mail.MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Falha ao montar o e-mail de redefinição de senha", e);
        }
    }

    private String textoPuro(String token, int validadeMinutos) {
        return """
                Plantão | Clínica Veterinária VidaPet

                Recebemos um pedido para redefinir a senha da sua conta no Plantão.

                Seu código:

                    %s

                O código vale por %d minutos e só pode ser usado uma vez. Pedir um código novo
                cancela este.

                Se não foi você que pediu, pode ignorar esta mensagem: nada muda na sua conta
                enquanto o código não for usado.

                Esta é uma mensagem automática do sistema Plantão. Não responda a este e-mail.
                """.formatted(token, validadeMinutos);
    }

    private String html(String token, int validadeMinutos) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <body style="margin:0;padding:24px 12px;background:#f3f6f4;
                             font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
                    <tr><td align="center">
                      <table role="presentation" width="520" cellpadding="0" cellspacing="0" border="0"
                             style="max-width:520px;width:100%%;background:#ffffff;border:1px solid #cdd8d4;
                                    border-radius:10px;overflow:hidden;">
                        <tr>
                          <td style="padding:20px 28px;background:%s;">
                            <div style="color:#ffffff;font-size:19px;font-weight:700;letter-spacing:-0.2px;">Plantão</div>
                            <div style="color:#d6efe7;font-size:13px;padding-top:2px;">Clínica Veterinária VidaPet</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px;">
                            <p style="margin:0 0 16px;font-size:15px;line-height:1.55;color:%s;">
                              Recebemos um pedido para redefinir a senha da sua conta no Plantão.
                            </p>
                            <p style="margin:0 0 10px;font-size:13px;color:%s;">Seu código:</p>
                            <div style="margin:0 0 18px;padding:16px;background:#e3f2ec;border:1px solid #bfdfd4;
                                        border-radius:8px;text-align:center;font-family:'Courier New',Courier,monospace;
                                        font-size:17px;font-weight:700;color:%s;word-break:break-all;">
                              %s
                            </div>
                            <p style="margin:0 0 16px;font-size:14px;line-height:1.55;color:%s;">
                              O código vale por <strong>%d minutos</strong> e só pode ser usado uma vez.
                              Pedir um código novo cancela este.
                            </p>
                            <p style="margin:0;font-size:14px;line-height:1.55;color:%s;">
                              Se não foi você que pediu, pode ignorar esta mensagem. Nada muda na sua conta
                              enquanto o código não for usado.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 28px;border-top:1px solid #e2eae6;background:#fbfdfc;">
                            <p style="margin:0;font-size:12px;line-height:1.5;color:%s;">
                              Mensagem automática do sistema Plantão. Não responda a este e-mail.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(VERDE, TINTA, TINTA_SUAVE, VERDE, token, TINTA, validadeMinutos, TINTA, TINTA_SUAVE);
    }
}
