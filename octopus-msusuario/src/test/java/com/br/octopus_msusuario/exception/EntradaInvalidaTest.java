package com.br.octopus_msusuario.exception;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entrada malformada é culpa de quem chamou, então tem que voltar 400. Antes destes testes, data fora
 * do padrão ISO, enum inexistente, JSON quebrado e id que não é UUID caíam no handler genérico e viravam 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EntradaInvalidaTest {

    @Autowired
    private MockMvc mockMvc;

    private String tokenAdmin() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@teste.local", "senha": "senha-de-teste"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    @Test
    @DisplayName("data fora do formato ISO: 400, não 500")
    void dataEmFormatoErradoRetorna400() throws Exception {
        mockMvc.perform(post("/api/recepcionistas")
                        .header("Authorization", tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "recep@teste.local", "senha": "teste123", "nome": "Guilherme",
                                 "cpf": "48449476801", "dataNascimento": "07-06-2003", "telefone": "11964834564"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("valor que não existe no enum: 400")
    void enumInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/veterinarios")
                        .header("Authorization", tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vet@teste.local", "senha": "teste123", "nome": "Dra. Carla",
                                 "cpfCnpj": "11122233344", "tipoPessoa": "PESSOA_FISICA",
                                 "dataNascimento": "1990-05-10", "telefone": "11999990000",
                                 "crmv": "77777", "crmvUf": "SP"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("JSON quebrado: 400")
    void jsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/api/recepcionistas")
                        .header("Authorization", tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("id que não é UUID: 400 dizendo qual parâmetro")
    void idInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/veterinarios/abc")
                        .header("Authorization", tokenAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Parâmetro 'id' inválido."));
    }

    @Test
    @DisplayName("campo obrigatório faltando continua 400 com os campos")
    void campoFaltandoRetorna400ComOsCampos() throws Exception {
        mockMvc.perform(post("/api/recepcionistas")
                        .header("Authorization", tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"c@d.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }
}
