package com.br.octopus_msusuario.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira com H2 (ver src/test/resources/application.properties) e exercita
 * login, filtro JWT e @PreAuthorize de ponta a ponta. O admin vem do AdminBootstrap.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutenticacaoEAutorizacaoTest {

    private static final String ADMIN_EMAIL = "admin@teste.local";
    private static final String ADMIN_SENHA = "senha-de-teste";

    @Autowired
    private MockMvc mockMvc;

    private String login(String email, String senha) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "%s"}
                                """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    /** Cadastra um veterinário como admin e devolve o id. Números aleatórios evitam colisão entre testes. */
    private String cadastrarVeterinario(String adminToken, String email) throws Exception {
        long cpf = ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L);
        int crmv = ThreadLocalRandom.current().nextInt(10_000, 99_999);
        String body = mockMvc.perform(post("/api/veterinarios")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "senha-forte-123", "nome": "Dra. Carla",
                                 "cpfCnpj": "%d", "tipoPessoa": "PF", "dataNascimento": "1990-05-10",
                                 "telefone": "11999990000", "crmv": "%d", "crmvUf": "SP"}
                                """.formatted(email, cpf, crmv)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.especializacao").value("GERAL"))
                .andExpect(jsonPath("$.usuario.role").value("ROLE_VETERINARIO"))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void semTokenRecebe401EmJson() throws Exception {
        mockMvc.perform(get("/api/veterinarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void senhaErradaRecebe401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "errada"}
                                """.formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginComCorpoInvalidoRecebe400ComOsCampos() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nao-e-email", "senha": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void adminCadastraVeterinarioEVeterinarioNaoAcessaRotaDeAdmin() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_SENHA);
        cadastrarVeterinario(admin, "vet.permissao@vidapet.com");

        String veterinario = login("vet.permissao@vidapet.com", "senha-forte-123");

        mockMvc.perform(get("/api/veterinarios").header("Authorization", veterinario))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/tutores").header("Authorization", veterinario))
                .andExpect(status().isForbidden());
    }

    @Test
    void emailDuplicadoRecebe409() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_SENHA);
        cadastrarVeterinario(admin, "vet.duplicado@vidapet.com");

        mockMvc.perform(post("/api/veterinarios")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vet.duplicado@vidapet.com", "senha": "senha-forte-123", "nome": "Outro",
                                 "cpfCnpj": "11122233344", "tipoPessoa": "PF", "dataNascimento": "1990-05-10",
                                 "telefone": "11999990000", "crmv": "1", "crmvUf": "RJ"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void desativarUsuarioInvalidaOsTokensJaEmitidos() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_SENHA);
        String id = cadastrarVeterinario(admin, "vet.desativado@vidapet.com");
        String tokenAntigo = login("vet.desativado@vidapet.com", "senha-forte-123");

        mockMvc.perform(patch("/api/veterinarios/{id}/desativar", id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.ativo").value(false));

        mockMvc.perform(get("/api/veterinarios").header("Authorization", tokenAntigo))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vet.desativado@vidapet.com", "senha": "senha-forte-123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCadastraTutorComAnimaisEAdicionaOutroDepois() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_SENHA);

        String body = mockMvc.perform(post("/api/tutores")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "João", "endereco": "Rua A, 10", "dataNascimento": "1985-01-01",
                                 "telefone": "11988887777",
                                 "animais": [{"nome": "Rex", "especie": "Cão", "dataAntirrabica": "2026-03-01"}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.animais[0].especie").value("Cão"))
                .andExpect(jsonPath("$.animais[0].dataUltimaAntirrabica").value("2026-03-01"))
                .andReturn().getResponse().getContentAsString();
        String tutorId = JsonPath.read(body, "$.id");

        mockMvc.perform(post("/api/tutores/{id}/animais", tutorId)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Mia", "especie": "Gato"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tutorId").value(tutorId));
    }

    @Test
    void animalSemEspecieRecebe400() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_SENHA);

        mockMvc.perform(post("/api/tutores")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "João", "endereco": "Rua A, 10", "dataNascimento": "1985-01-01",
                                 "telefone": "11988887777", "animais": [{"nome": "Rex"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos['animais[0].especie']").exists());
    }

    @Test
    void animalParaTutorInexistenteRecebe404() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_SENHA);

        mockMvc.perform(post("/api/tutores/{id}/animais", java.util.UUID.randomUUID())
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Mia", "especie": "Gato"}
                                """))
                .andExpect(status().isNotFound());
    }
}
