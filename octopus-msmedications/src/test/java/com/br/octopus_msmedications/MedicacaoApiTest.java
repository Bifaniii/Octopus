package com.br.octopus_msmedications;

import com.br.octopus_msmedications.domain.enums.TipoEsquema;
import com.br.octopus_msmedications.dto.request.MedicacaoRequest;
import com.br.octopus_msmedications.repository.MedicacaoRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MedicacaoApiTest {

    private static final String URL = "/api/medicacoes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicacaoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.security.jwt.secret}")
    private String segredoBase64;

    @BeforeEach
    void limpar() {
        // Direto no banco: apagar via repository esbarraria no lazy loading da coleção de interações.
        jdbcTemplate.execute("DELETE FROM tb_medicacao_interacoes");
        jdbcTemplate.execute("DELETE FROM tb_medicacoes");
    }

    // Imita o token que o octopus-msusuario emite: subject = e-mail, claim "role".
    private String token(String role) {
        var chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredoBase64));
        return Jwts.builder()
                .subject("usuario@octopus.local")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(chave)
                .compact();
    }

    private MedicacaoRequest novaRequest(String nome, String registroAnvisa, Set<UUID> interacoes) {
        return new MedicacaoRequest(nome, "Dipirona monoidratada", "500mg", "Comprimido", "mg",
                TipoEsquema.SINTOMATICO, LocalDateTime.now().plusYears(1), "Sanofi", registroAnvisa, interacoes);
    }

    private UUID criar(String nome, String registroAnvisa) throws Exception {
        String corpo = mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_VETERINARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novaRequest(nome, registroAnvisa, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(corpo).get("id").asString());
    }

    @Test
    @DisplayName("sem token: 401")
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token inválido: 401")
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("auxiliar lê, mas não cadastra: 200 na listagem e 403 no POST")
    void auxiliarNaoCadastra() throws Exception {
        String tokenAuxiliar = token("ROLE_AUXILIAR");

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAuxiliar))
                .andExpect(status().isOk());

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAuxiliar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novaRequest("Novalgina", "12345678901", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinário cadastra e o registro ANVISA duplicado dá 409")
    void cadastroEDuplicidade() throws Exception {
        criar("Novalgina", "12345678901");

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_VETERINARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novaRequest("Outro nome", "12345678901", null))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("dados inválidos: 400 com o campo que falhou")
    void validacaoRetorna400() throws Exception {
        var invalida = new MedicacaoRequest("", "Dipirona", "500mg", "Comprimido", "mg",
                TipoEsquema.CONTINUO, LocalDateTime.now().plusYears(1), "Sanofi", "123", null);

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_VETERINARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nomeComercial").exists())
                .andExpect(jsonPath("$.campos.numeroRegistroAnvisa").exists());
    }

    @Test
    @DisplayName("id inexistente: 404")
    void buscaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get(URL + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_VETERINARIO")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("interação proibida vale nos dois sentidos")
    void interacaoESimetrica() throws Exception {
        UUID dipirona = criar("Novalgina", "12345678901");
        String tokenVet = token("ROLE_VETERINARIO");

        // Cadastra a segunda já apontando a primeira como interação proibida.
        var comInteracao = novaRequest("Tramal", "10987654321", Set.of(dipirona));
        String corpo = mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVet)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comInteracao)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interacoesProibidas[0].id").value(dipirona.toString()))
                .andReturn().getResponse().getContentAsString();
        UUID tramal = UUID.fromString(objectMapper.readTree(corpo).get("id").asString());

        // O outro lado do par também enxerga a proibição.
        mockMvc.perform(get(URL + "/" + dipirona).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVet))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interacoesProibidas[0].id").value(tramal.toString()));
    }

    @Test
    @DisplayName("interação consigo mesmo: 422")
    void interacaoConsigoMesmoRetorna422() throws Exception {
        UUID id = criar("Novalgina", "12345678901");

        String corpo = """
                {"interacoesProibidas": ["%s"]}""".formatted(id);

        mockMvc.perform(patch(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_VETERINARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH altera só o que foi enviado")
    void patchAlteraApenasOsCamposEnviados() throws Exception {
        UUID id = criar("Novalgina", "12345678901");

        mockMvc.perform(patch(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_VETERINARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoEsquema\": \"CONTINUO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoEsquema").value("CONTINUO"))
                .andExpect(jsonPath("$.nomeComercial").value("Novalgina"));
    }

    @Test
    @DisplayName("só admin remove: veterinário recebe 403 e admin 204")
    void remocaoSoPeloAdmin() throws Exception {
        UUID id = criar("Novalgina", "12345678901");

        mockMvc.perform(delete(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_VETERINARIO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        assertThat(repository.findById(id)).isEmpty();
    }
}
