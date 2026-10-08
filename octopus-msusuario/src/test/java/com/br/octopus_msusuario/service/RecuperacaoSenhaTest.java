package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.TokenRecuperacaoSenha;
import com.br.octopus_msusuario.domain.enums.StatusTokenSenha;
import com.br.octopus_msusuario.mail.EmailService;
import com.br.octopus_msusuario.repository.TokenRecuperacaoSenhaRepository;
import com.br.octopus_msusuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de redefinição de senha de ponta a ponta, com H2. O JavaMailSender é substituído por um mock,
 * então nada é enviado de verdade e dá para capturar o código que iria no e-mail.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecuperacaoSenhaTest {

    private static final String ADMIN_EMAIL = "admin@teste.local";
    private static final String ADMIN_SENHA = "senha-de-teste";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacaoSenhaRepository tokenRepository;

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpar() {
        tokenRepository.deleteAll();
        reset(emailService);
    }

    private void pedirCodigo(String email) throws Exception {
        mockMvc.perform(post("/api/auth/esqueci-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}
                                """.formatted(email)))
                .andExpect(status().isNoContent());
    }

    /** Lê o código que o serviço mandou para o EmailService. */
    private String codigoEnviado() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarEmailRedefinicaoSenha(anyString(), captor.capture(), anyInt());
        return captor.getValue();
    }

    private void redefinir(String token, String novaSenha, int statusEsperado) throws Exception {
        mockMvc.perform(post("/api/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s", "novaSenha": "%s"}
                                """.formatted(token, novaSenha)))
                .andExpect(status().is(statusEsperado));
    }

    private void login(String email, String senha, int statusEsperado) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "%s"}
                                """.formatted(email, senha)))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    @DisplayName("os dois endpoints são públicos: funcionam sem token de login")
    void endpointsSaoPublicos() throws Exception {
        pedirCodigo(ADMIN_EMAIL);
        redefinir("codigo-que-nao-existe", "senha-nova-123", 400);
    }

    @Test
    @DisplayName("e-mail sem conta recebe o mesmo 204, e nenhum e-mail é enviado")
    void emailInexistenteNaoVazaNemEnvia() throws Exception {
        pedirCodigo("ninguem@teste.local");

        verify(emailService, never()).enviarEmailRedefinicaoSenha(anyString(), anyString(), anyInt());
        assertThat(tokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("pedido válido grava um token ATIVO e manda o código por e-mail")
    void pedidoValidoGeraTokenAtivo() throws Exception {
        pedirCodigo(ADMIN_EMAIL);

        String codigo = codigoEnviado();
        List<TokenRecuperacaoSenha> tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getToken()).isEqualTo(codigo);
        assertThat(tokens.get(0).getStatus()).isEqualTo(StatusTokenSenha.ATIVO);
        assertThat(tokens.get(0).getExpiraEm()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("o código tem sempre 6 dígitos, inclusive quando sorteia um número pequeno")
    void codigoTemSeisDigitos() throws Exception {
        // Roda algumas vezes porque o zero à esquerda só aparece em parte dos sorteios: sem o %06d,
        // um sorteio como 4821 viraria um código de 4 dígitos.
        for (int i = 0; i < 20; i++) {
            jdbcTemplate.execute("DELETE FROM tb_tokens_recuperacao_senha");
            reset(emailService);
            pedirCodigo(ADMIN_EMAIL);
            assertThat(codigoEnviado()).matches("\\d{6}");
        }
    }

    @Test
    @DisplayName("pedir de novo arquiva o código anterior e emite outro")
    void pedidoNovoArquivaOAnterior() throws Exception {
        pedirCodigo(ADMIN_EMAIL);
        String primeiro = codigoEnviado();
        reset(emailService);

        pedirCodigo(ADMIN_EMAIL);
        String segundo = codigoEnviado();

        assertThat(segundo).isNotEqualTo(primeiro);
        assertThat(tokenRepository.findAll()).hasSize(2);
        assertThat(tokenRepository.findByTokenAndStatus(primeiro, StatusTokenSenha.ATIVO)).isEmpty();
        assertThat(tokenRepository.findAll().stream()
                .filter(t -> t.getToken().equals(primeiro))
                .findFirst()
                .orElseThrow()
                .getStatus()).isEqualTo(StatusTokenSenha.ARQUIVADO);
    }

    @Test
    @DisplayName("o código arquivado não redefine mais nada")
    void codigoArquivadoNaoFunciona() throws Exception {
        pedirCodigo(ADMIN_EMAIL);
        String primeiro = codigoEnviado();
        reset(emailService);
        pedirCodigo(ADMIN_EMAIL);

        redefinir(primeiro, "senha-nova-123", 400);
        login(ADMIN_EMAIL, ADMIN_SENHA, 200);
    }

    @Test
    @DisplayName("código válido troca a senha: a antiga para de valer e a nova entra")
    void redefinicaoTrocaASenha() throws Exception {
        pedirCodigo(ADMIN_EMAIL);
        String codigo = codigoEnviado();

        redefinir(codigo, "senha-nova-123", 204);

        login(ADMIN_EMAIL, ADMIN_SENHA, 401);
        login(ADMIN_EMAIL, "senha-nova-123", 200);

        // Devolve a senha original para não atrapalhar os outros testes da suíte.
        pedirCodigo(ADMIN_EMAIL);
        reset(emailService);
        pedirCodigo(ADMIN_EMAIL);
        redefinir(codigoEnviado(), ADMIN_SENHA, 204);
    }

    @Test
    @DisplayName("o mesmo código não serve duas vezes")
    void codigoUsadoNaoRepete() throws Exception {
        pedirCodigo(ADMIN_EMAIL);
        String codigo = codigoEnviado();

        redefinir(codigo, ADMIN_SENHA, 204);
        redefinir(codigo, "outra-senha-123", 400);

        assertThat(tokenRepository.findAll().get(0).getStatus()).isEqualTo(StatusTokenSenha.USADO);
        assertThat(tokenRepository.findAll().get(0).getUsadoEm()).isNotNull();
    }

    @Test
    @DisplayName("código expirado dá 400 e é arquivado")
    void codigoExpiradoNaoFunciona() throws Exception {
        pedirCodigo(ADMIN_EMAIL);
        String codigo = codigoEnviado();

        TokenRecuperacaoSenha token = tokenRepository.findByTokenAndStatus(codigo, StatusTokenSenha.ATIVO).orElseThrow();
        token.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(token);

        redefinir(codigo, "senha-nova-123", 400);
        login(ADMIN_EMAIL, ADMIN_SENHA, 200);
    }

    @Test
    @DisplayName("e-mail inválido e senha curta: 400")
    void validacaoDosCampos() throws Exception {
        mockMvc.perform(post("/api/auth/esqueci-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nao-e-email\"}"))
                .andExpect(status().isBadRequest());

        pedirCodigo(ADMIN_EMAIL);
        redefinir(codigoEnviado(), "123", 400);
    }

    @Test
    @DisplayName("SMTP fora do ar não derruba o pedido: o token fica gravado")
    void falhaNoEnvioNaoDerrubaOPedido() throws Exception {
        doThrow(new RuntimeException("conexão recusada"))
                .when(emailService).enviarEmailRedefinicaoSenha(anyString(), anyString(), anyInt());

        pedirCodigo(ADMIN_EMAIL);

        verify(emailService, times(1)).enviarEmailRedefinicaoSenha(anyString(), anyString(), anyInt());
        assertThat(tokenRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("conta desativada não recebe código")
    void contaDesativadaNaoRecebeCodigo() throws Exception {
        var usuario = usuarioRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);

        try {
            pedirCodigo(ADMIN_EMAIL);
            verify(emailService, never()).enviarEmailRedefinicaoSenha(anyString(), anyString(), anyInt());
            assertThat(tokenRepository.findAll()).isEmpty();
        } finally {
            usuario.setAtivo(true);
            usuarioRepository.save(usuario);
        }
    }
}
