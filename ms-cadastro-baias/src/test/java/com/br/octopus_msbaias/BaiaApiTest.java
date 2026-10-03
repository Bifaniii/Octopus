package com.br.octopus_msbaias;

import com.br.octopus_msbaias.domain.enums.Tipo;
import com.br.octopus_msbaias.dto.request.BaiaRequest;
import com.br.octopus_msbaias.repository.BaiaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BaiaApiTest {

    private static final String URL = "/api/baias";
    private static final int LIMITE_BAIAS = 12;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BaiaRepository repository;

    @Value("${app.security.jwt.secret}")
    private String segredoBase64;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    // Mesmo formato do token do octopus-msusuario: subject = e-mail, claim "role".
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

    private UUID criar(String nome, Tipo tipo, int capacidade) throws Exception {
        String corpo = mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BaiaRequest(tipo, nome, "descrição", capacidade))))
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
    @DisplayName("veterinário lê, mas não cadastra: 200 na listagem e 403 no POST")
    void veterinarioNaoCadastra() throws Exception {
        String tokenVet = token("ROLE_VETERINARIO");

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVet))
                .andExpect(status().isOk());

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenVet)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BaiaRequest(Tipo.COLETIVA, "Baia 1", null, 4))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin cadastra e busca por id")
    void adminCadastraEBusca() throws Exception {
        UUID id = criar("Baia 1", Tipo.COLETIVA, 4);

        mockMvc.perform(get(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Baia 1"))
                .andExpect(jsonPath("$.tipo").value("COLETIVA"))
                .andExpect(jsonPath("$.capacidade").value(4))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("nome repetido, ignorando maiúsculas: 409")
    void nomeDuplicadoRetorna409() throws Exception {
        criar("Baia 1", Tipo.COLETIVA, 4);

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BaiaRequest(Tipo.NINHADA, "baia 1", null, 3))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("isolamento com capacidade acima de 1: 422")
    void capacidadeAcimaDoTipoRetorna422() throws Exception {
        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BaiaRequest(Tipo.ISOLAMENTO, "Isolamento 1", null, 2))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("dados inválidos: 400 com os campos que falharam")
    void validacaoRetorna400() throws Exception {
        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BaiaRequest(Tipo.COLETIVA, "", null, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.capacidade").exists());
    }

    @Test
    @DisplayName("id inexistente: 404")
    void buscaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get(URL + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("edição troca tipo, nome e capacidade")
    void atualizarAlteraOsCampos() throws Exception {
        UUID id = criar("Baia 1", Tipo.COLETIVA, 4);

        mockMvc.perform(put(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BaiaRequest(Tipo.ISOLAMENTO, "Isolamento 1", "reformada", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ISOLAMENTO"))
                .andExpect(jsonPath("$.nome").value("Isolamento 1"))
                .andExpect(jsonPath("$.capacidade").value(1));
    }

    @Test
    @DisplayName("desativar arquiva sem apagar: o registro continua no banco")
    void desativarNaoApaga() throws Exception {
        UUID id = criar("Baia 1", Tipo.COLETIVA, 4);

        mockMvc.perform(patch(URL + "/" + id + "/desativar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        assertThat(repository.findById(id)).isPresent();
    }

    @Test
    @DisplayName("ativar devolve a baia arquivada ao uso")
    void ativarVoltaABaia() throws Exception {
        UUID id = criar("Baia 1", Tipo.COLETIVA, 4);
        String tokenAdmin = token("ROLE_ADMIN");

        mockMvc.perform(patch(URL + "/" + id + "/desativar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(patch(URL + "/" + id + "/ativar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("a clínica tem 12 baias: a décima terceira recebe 422")
    void limiteDeDozeBaiasAtivas() throws Exception {
        for (int i = 1; i <= LIMITE_BAIAS; i++) {
            criar("Baia " + i, Tipo.COLETIVA, 4);
        }

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BaiaRequest(Tipo.COLETIVA, "Baia 13", null, 4))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("reativar também respeita o limite de 12 ativas")
    void ativarAcimaDoLimiteRetorna422() throws Exception {
        String tokenAdmin = token("ROLE_ADMIN");
        for (int i = 1; i <= LIMITE_BAIAS; i++) {
            criar("Baia " + i, Tipo.COLETIVA, 4);
        }

        // Abre uma vaga arquivando a primeira, usa a vaga numa baia nova e tenta reativar a arquivada.
        UUID arquivada = repository.findAll().get(0).getId();
        mockMvc.perform(patch(URL + "/" + arquivada + "/desativar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        criar("Baia 13", Tipo.COLETIVA, 4);

        mockMvc.perform(patch(URL + "/" + arquivada + "/ativar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isUnprocessableEntity());
    }
}
