package com.br.octopus_msprescricao.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira com H2 (ver src/test/resources/application.properties) e exercita o filtro JWT,
 * o @PreAuthorize e o fluxo de prescrição de ponta a ponta. Os tokens são emitidos por {@link TokensDeTeste},
 * como o msusuario faria. UUIDs aleatórios de internação isolam um teste do outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PrescricaoControllerTest {

    private static final String VET = TokensDeTeste.bearer("vet@teste.local", "ROLE_VETERINARIO");
    private static final String AUXILIAR = TokensDeTeste.bearer("aux@teste.local", "ROLE_AUXILIAR");

    @Autowired
    private MockMvc mockMvc;

    private static String corpoValido(UUID internacaoId) {
        return """
                {"internacaoId": "%s", "observacao": "Animal agitado",
                 "itens": [{"medicacaoId": "%s", "dosagem": "5 mg", "intervaloHoras": 8,
                            "quantidadeDoses": 3, "inicio": "2026-10-10T08:00:00"}]}
                """.formatted(internacaoId, UUID.randomUUID());
    }

    /** Cria uma prescrição como veterinário e devolve o id. */
    private String criarPrescricao(UUID internacaoId) throws Exception {
        String body = mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(internacaoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void semTokenRecebe401EmJson() throws Exception {
        mockMvc.perform(get("/api/prescricoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenInvalidoRecebe401() throws Exception {
        mockMvc.perform(get("/api/prescricoes").header("Authorization", "Bearer lixo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auxiliarNaoPodePrescreverMasPodeLer() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", AUXILIAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/prescricoes").header("Authorization", AUXILIAR))
                .andExpect(status().isOk());
    }

    @Test
    void veterinarioPrescreveEAsDosesSaoGeradas() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.veterinarioEmail").value("vet@teste.local"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].doses.length()").value(3))
                .andExpect(jsonPath("$.itens[0].doses[0].status").value("PENDENTE"));
    }

    @Test
    void corpoSemInternacaoENemItensRecebe400ComOsCampos() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.internacaoId").exists())
                .andExpect(jsonPath("$.campos.itens").exists());
    }

    @Test
    void intervaloZeroRecebe400ApontandoOCampoDoItem() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"internacaoId": "%s",
                                 "itens": [{"medicacaoId": "%s", "dosagem": "5 mg", "intervaloHoras": 0,
                                            "quantidadeDoses": 3, "inicio": "2026-10-10T08:00:00"}]}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos['itens[0].intervaloHoras']").exists());
    }

    @Test
    void horaForaDoPadraoRecebe400() throws Exception {
        mockMvc.perform(post("/api/prescricoes")
                        .header("Authorization", VET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"internacaoId": "%s",
                                 "itens": [{"medicacaoId": "%s", "dosagem": "5 mg", "intervaloHoras": 8,
                                            "quantidadeDoses": 3, "inicio": "amanhã cedo"}]}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarPorInternacaoDevolveSoAsDaquelaInternacao() throws Exception {
        UUID internacaoId = UUID.randomUUID();
        criarPrescricao(internacaoId);
        criarPrescricao(UUID.randomUUID());

        mockMvc.perform(get("/api/prescricoes").param("internacaoId", internacaoId.toString())
                        .header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].internacaoId").value(internacaoId.toString()));
    }

    @Test
    void buscarPorIdDevolveAPrescricaoComItensEDoses() throws Exception {
        String id = criarPrescricao(UUID.randomUUID());

        mockMvc.perform(get("/api/prescricoes/" + id).header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.itens[0].doses.length()").value(3));
    }

    @Test
    void buscarInexistenteRecebe404() throws Exception {
        mockMvc.perform(get("/api/prescricoes/" + UUID.randomUUID()).header("Authorization", VET))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void idQueNaoEUuidRecebe400() throws Exception {
        mockMvc.perform(get("/api/prescricoes/abc").header("Authorization", VET))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desativarArquivaAPrescricaoSemApagar() throws Exception {
        String id = criarPrescricao(UUID.randomUUID());

        mockMvc.perform(patch("/api/prescricoes/" + id + "/desativar").header("Authorization", AUXILIAR))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/prescricoes/" + id + "/desativar").header("Authorization", VET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // continua listada, agora marcada como inativa
        mockMvc.perform(get("/api/prescricoes/" + id).header("Authorization", VET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }
}
