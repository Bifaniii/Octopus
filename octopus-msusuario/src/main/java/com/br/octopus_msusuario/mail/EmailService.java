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

    // Tema escuro do front, copiado de src/styles/tema.css (html.dark-theme) do Octopus-front.
    // O nome ao lado é a custom property correspondente, para facilitar quando a paleta mudar lá.
    private static final String AZUL = "#3b82f6";                   // --color-primary
    private static final String FUNDO = "#0b1220";                  // --color-bg
    private static final String SUPERFICIE = "#141b2d";             // --color-surface
    private static final String SUPERFICIE_ALT = "#0f172a";         // --color-surface-alt
    private static final String TEXTO = "#f1f5f9";                  // --color-text
    private static final String TEXTO_SUAVE = "#94a3b8";            // --color-text-muted
    private static final String TEXTO_SOBRE_AZUL = "#f1f5f9";       // --color-text-on-primary
    private static final String TEXTO_SOBRE_AZUL_SUAVE = "#e2e8f0"; // --color-text-on-primary-muted
    private static final String BORDA = "#334155";                  // --color-border

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    /**
     * Envia o código em multipart: texto puro e HTML. Cliente que não renderiza HTML lê a versão em texto, e
     * mandar as duas reduz a chance de o filtro tratar a mensagem como spam.
     */
    public void enviarEmailRedefinicaoSenha(String destinatario, String codigo, int validadeMinutos) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente, NOME_REMETENTE);
            helper.setTo(destinatario);
            helper.setSubject("Seu código para redefinir a senha do Plantão");
            helper.setText(textoPuro(codigo, validadeMinutos), html(codigo, validadeMinutos));
            mailSender.send(mime);
        } catch (jakarta.mail.MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Falha ao montar o e-mail de redefinição de senha", e);
        }
    }

    private String textoPuro(String codigo, int validadeMinutos) {
        return """
                Plantão | Clínica Veterinária VidaPet

                Recebemos um pedido para redefinir a senha da sua conta no Plantão.

                Seu código de %d dígitos:

                    %s

                O código vale por %d minutos e só pode ser usado uma vez. Pedir um código novo
                cancela este.

                Se não foi você que pediu, pode ignorar esta mensagem: nada muda na sua conta
                enquanto o código não for usado.

                Esta é uma mensagem automática do sistema Plantão. Não responda a este e-mail.
                """.formatted(codigo.length(), codigo, validadeMinutos);
    }

    /**
     * Os marcadores são indexados (%1$s, %2$s...) de propósito: com quase vinte cores no template, a ordem
     * posicional seria fácil de quebrar numa edição futura.
     */
    private String html(String codigo, int validadeMinutos) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <body style="margin:0;padding:24px 12px;background:%1$s;
                             font-family:'Poppins',system-ui,-apple-system,'Segoe UI',Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"
                         style="background:%1$s;">
                    <tr><td align="center">
                      <table role="presentation" width="520" cellpadding="0" cellspacing="0" border="0"
                             style="max-width:520px;width:100%%;background:%2$s;border:1px solid %3$s;
                                    border-radius:14px;overflow:hidden;">
                        <tr>
                          <td style="padding:20px 28px;background:%4$s;">
                            <div style="color:%5$s;font-size:19px;font-weight:700;letter-spacing:-0.2px;">Plantão</div>
                            <div style="color:%6$s;font-size:13px;padding-top:2px;">Clínica Veterinária VidaPet</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px;">
                            <p style="margin:0 0 18px;font-size:15px;line-height:1.55;color:%7$s;">
                              Recebemos um pedido para redefinir a senha da sua conta no Plantão.
                            </p>
                            <p style="margin:0 0 10px;font-size:13px;color:%8$s;">Digite este código no Plantão:</p>
                            <div style="margin:0 0 18px;padding:18px 12px;background:%9$s;border:1px solid %4$s;
                                        border-radius:10px;text-align:center;">
                              <span style="font-family:'Courier New',Courier,monospace;font-size:30px;
                                           font-weight:700;letter-spacing:8px;color:%4$s;">%10$s</span>
                            </div>
                            <p style="margin:0 0 16px;font-size:14px;line-height:1.55;color:%7$s;">
                              O código vale por <strong style="color:%7$s;">%11$d minutos</strong> e só pode ser
                              usado uma vez. Pedir um código novo cancela este.
                            </p>
                            <p style="margin:0;font-size:14px;line-height:1.55;color:%8$s;">
                              Se não foi você que pediu, pode ignorar esta mensagem. Nada muda na sua conta
                              enquanto o código não for usado.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 28px;border-top:1px solid %3$s;background:%9$s;">
                            <p style="margin:0;font-size:12px;line-height:1.5;color:%8$s;">
                              Mensagem automática do sistema Plantão. Não responda a este e-mail.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(FUNDO, SUPERFICIE, BORDA, AZUL, TEXTO_SOBRE_AZUL, TEXTO_SOBRE_AZUL_SUAVE,
                              TEXTO, TEXTO_SUAVE, SUPERFICIE_ALT, codigo, validadeMinutos);
    }
}
