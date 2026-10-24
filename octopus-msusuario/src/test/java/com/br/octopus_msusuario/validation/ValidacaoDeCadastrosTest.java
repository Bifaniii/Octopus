package com.br.octopus_msusuario.validation;

import com.br.octopus_msusuario.GeradorDeDocumentos;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CPF e CNPJ com dígito verificador, celular, CRMV de até 5 dígitos e UF como enum: regras que o back recusa com 400
 * e a mensagem no campo certo, mesmo que o front deixe passar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValidacaoDeCadastrosTest {

    @Autowired
    private MockMvc mockMvc;

    private String admin;

    @BeforeEach
    void entrar() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"admin@teste.local\", \"senha\": \"senha-de-teste\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        admin = "Bearer " + JsonPath.read(body, "$.token");
    }

    /** CRMV novo a cada chamada: o mesmo número com a mesma UF é 409, e o contexto do teste é compartilhado. */
    private static String crmv() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(10_000, 99_999));
    }

    private ResultActions veterinario(String cpfCnpj, String tipo, String telefone, String crmv, String uf) throws Exception {
        String email = "vet" + ThreadLocalRandom.current().nextInt(1_000_000) + "@teste.local";
        return mockMvc.perform(post("/api/veterinarios").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "senha": "senha-forte-123", "nome": "Dra. Carla", "cpfCnpj": "%s",
                         "tipoPessoa": "%s", "dataNascimento": "1990-05-10", "telefone": "%s",
                         "crmv": "%s", "crmvUf": "%s"}
                        """.formatted(email, cpfCnpj, tipo, telefone, crmv, uf)));
    }

    private ResultActions auxiliar(String cpf, String telefone) throws Exception {
        String email = "aux" + ThreadLocalRandom.current().nextInt(1_000_000) + "@teste.local";
        return mockMvc.perform(post("/api/auxiliares").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "senha": "senha-forte-123", "nome": "Paulo", "cpf": "%s",
                         "dataNascimento": "1995-01-01", "telefone": "%s"}
                        """.formatted(email, cpf, telefone)));
    }

    @Test
    @DisplayName("veterinário válido: CPF certo, celular, CRMV de 5 dígitos e UF do enum")
    void veterinarioValido() throws Exception {
        String crmv = crmv();
        veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", crmv, "SP")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.crmv").value(crmv))
                .andExpect(jsonPath("$.crmvUf").value("SP"));
    }

    @Test
    @DisplayName("CNPJ certo para pessoa jurídica")
    void veterinarioPessoaJuridica() throws Exception {
        veterinario(GeradorDeDocumentos.cnpj(), "PJ", "21987654321", crmv(), "RJ").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CPF com dígito verificador errado: 400 no campo cpfCnpj")
    void cpfComDigitoErrado() throws Exception {
        String cpf = GeradorDeDocumentos.cpf();
        String errado = cpf.substring(0, 10) + (cpf.charAt(10) == '0' ? '1' : '0');
        veterinario(errado, "PF", "11987654321", "12345", "SP")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpfCnpj").value(org.hamcrest.Matchers.containsString("CPF inválido")));
    }

    @Test
    @DisplayName("sequência repetida não é CPF")
    void cpfComTodosDigitosIguais() throws Exception {
        veterinario("11111111111", "PF", "11987654321", "12345", "SP").andExpect(status().isBadRequest());
        auxiliar("00000000000", "11987654321").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpf").exists());
    }

    @Test
    @DisplayName("tipo de pessoa e documento têm de combinar")
    void documentoNaoCombinaComOTipo() throws Exception {
        veterinario(GeradorDeDocumentos.cpf(), "PJ", "11987654321", "12345", "SP")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpfCnpj").value(org.hamcrest.Matchers.containsString("CNPJ inválido")));
        veterinario(GeradorDeDocumentos.cnpj(), "PF", "11987654321", "12345", "SP")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpfCnpj").value(org.hamcrest.Matchers.containsString("CPF inválido")));
    }

    @Test
    @DisplayName("CRMV: no máximo 5 dígitos, e só números")
    void crmvComAteCincoDigitos() throws Exception {
        veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", "123456", "SP")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.crmv").exists());
        veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", "12A45", "SP")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.crmv").exists());
        veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", "", "SP")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.crmv").exists());
        veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", "7", "MG").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("UF é um enum: sigla que não existe é 400, minúscula também")
    void ufForaDoEnum() throws Exception {
        veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", "12345", "XX").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.crmvUf").value(org.hamcrest.Matchers.containsString("SP")))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.startsWith("Campo 'crmvUf'")));
        veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", "12345", "sp").andExpect(status().isBadRequest());
        veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", "12345", "").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("telefone: só celular com DDD que existe")
    void soCelular() throws Exception {
        auxiliar(GeradorDeDocumentos.cpf(), "1133334444").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.telefone").value(org.hamcrest.Matchers.containsString("Celular inválido")));
        auxiliar(GeradorDeDocumentos.cpf(), "10987654321").andExpect(status().isBadRequest()); // DDD 10 não existe
        auxiliar(GeradorDeDocumentos.cpf(), "11887654321").andExpect(status().isBadRequest()); // sem o 9
        auxiliar(GeradorDeDocumentos.cpf(), "(11) 98765-4321").andExpect(status().isBadRequest()); // com máscara
        auxiliar(GeradorDeDocumentos.cpf(), "119876543210").andExpect(status().isBadRequest()); // 12 dígitos
        auxiliar(GeradorDeDocumentos.cpf(), "11987654321").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("tutor também só aceita celular")
    void tutorSoComCelular() throws Exception {
        String corpo = """
                {"nome": "João", "endereco": "Rua A, 10", "dataNascimento": "1985-01-01", "telefone": "%s"}
                """;
        mockMvc.perform(post("/api/tutores").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted("1133334444")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.telefone").exists());
        mockMvc.perform(post("/api/tutores").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted("11988887777")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("edição aplica as mesmas regras")
    void edicaoComAsMesmasRegras() throws Exception {
        String id = JsonPath.read(veterinario(GeradorDeDocumentos.cpf(), "PF", "11987654321", crmv(), "SP")
                .andReturn().getResponse().getContentAsString(), "$.id");
        String corpo = """
                {"email": "edit%d@teste.local", "nome": "Dra. Carla", "cpfCnpj": "%s", "tipoPessoa": "PF",
                 "dataNascimento": "1990-05-10", "telefone": "%s", "crmv": "%s", "crmvUf": "%s"}
                """;
        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        mockMvc.perform(put("/api/veterinarios/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted(n, "12345678901", "11987654321", "12345", "SP")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.cpfCnpj").exists());
        mockMvc.perform(put("/api/veterinarios/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted(n, GeradorDeDocumentos.cpf(), "1133334444", "12345", "SP")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.telefone").exists());
        mockMvc.perform(put("/api/veterinarios/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted(n, GeradorDeDocumentos.cpf(), "11987654321", "123456", "SP")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.crmv").exists());
    }

    @Test
    void documentosUtilitarios() {
        assertThat(Documentos.cpfValido("52998224725")).isTrue();
        assertThat(Documentos.cpfValido("52998224726")).isFalse();
        assertThat(Documentos.cpfValido("529.982.247-25")).isFalse(); // com máscara não vale
        assertThat(Documentos.cnpjValido("11222333000181")).isTrue();
        assertThat(Documentos.cnpjValido("11222333000182")).isFalse();
        assertThat(Documentos.cnpjValido("00000000000000")).isFalse();
        assertThat(Documentos.cpfValido(null)).isFalse();
    }
}
