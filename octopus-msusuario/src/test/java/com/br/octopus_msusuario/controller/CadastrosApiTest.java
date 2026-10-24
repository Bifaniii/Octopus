package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.GeradorDeDocumentos;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tutores, animais (com a mãe) e a listagem de usuários com filtros, de ponta a ponta com H2. O contexto é
 * compartilhado com as outras classes de teste, então os nomes levam um sufixo aleatório.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CadastrosApiTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.br.octopus_msusuario.mail.EmailService emailService;

    private String admin;
    private String sufixo;

    @BeforeEach
    void entrar() throws Exception {
        admin = login("admin@teste.local", "senha-de-teste");
        sufixo = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 999_999));
    }

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

    private long criarTutor(String nome) throws Exception {
        String body = mockMvc.perform(post("/api/tutores")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "endereco": "Rua A, 10", "dataNascimento": "1980-01-01",
                                 "telefone": "11999990000"}
                                """.formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long criarAnimal(long tutorId, String nome, Long maeId) throws Exception {
        String body = mockMvc.perform(post("/api/animais").param("tutorId", String.valueOf(tutorId))
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "especie": "Cão", "dataAntirrabica": "2026-03-01", "maeId": %s}
                                """.formatted(nome, maeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tutorId").value(tutorId))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String corpoAnimal(String nome, Long maeId) {
        return """
                {"nome": "%s", "especie": "Cão", "dataAntirrabica": "2026-05-10", "maeId": %s}
                """.formatted(nome, maeId);
    }

    @Test
    void postEmAnimaisCriaOAnimalParaOTutorDaQueryString() throws Exception {
        long tutor = criarTutor("Márcia " + sufixo);
        long mae = criarAnimal(tutor, "Nina", null);

        long filhote = criarAnimal(tutor, "Bidu", mae);

        mockMvc.perform(get("/api/animais/{id}", filhote).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maeId").value(mae));
    }

    @Test
    void postEmAnimaisSemCorpoValidoRecebe400() throws Exception {
        long tutor = criarTutor("Paulo " + sufixo);

        mockMvc.perform(post("/api/animais").param("tutorId", String.valueOf(tutor))
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Thor\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.especie").exists());
    }

    @Test
    void putEmAnimalTrocaDadosEMae() throws Exception {
        long tutor = criarTutor("Cleusa " + sufixo);
        long mae = criarAnimal(tutor, "Lua", null);
        long animal = criarAnimal(tutor, "Bidu", null);

        mockMvc.perform(put("/api/animais/{id}", animal)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAnimal("Bidu Jr", mae)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Bidu Jr"))
                .andExpect(jsonPath("$.dataUltimaAntirrabica").value("2026-05-10"))
                .andExpect(jsonPath("$.maeId").value(mae));
    }

    @Test
    void animalNaoPodeSerMaeDeSiMesmoNemDaPropriaMae() throws Exception {
        long tutor = criarTutor("Rodrigo " + sufixo);
        long mae = criarAnimal(tutor, "Lua", null);
        long filhote = criarAnimal(tutor, "Sol", mae);

        mockMvc.perform(put("/api/animais/{id}", filhote)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAnimal("Sol", filhote)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/api/animais/{id}", mae)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAnimal("Lua", filhote)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void putEmTutorAtualizaOsDadosEMantemOsAnimais() throws Exception {
        long tutor = criarTutor("Ana " + sufixo);
        criarAnimal(tutor, "Mel", null);

        mockMvc.perform(put("/api/tutores/{id}", tutor)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana Souza", "endereco": "Rua B, 20", "dataNascimento": "1981-02-02",
                                 "telefone": "11988887777"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.endereco").value("Rua B, 20"))
                .andExpect(jsonPath("$.animais.length()").value(1));
    }

    private String cadastrarAuxiliar(String nome) throws Exception {
        String cpf = GeradorDeDocumentos.cpf();
        String body = mockMvc.perform(post("/api/auxiliares")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s@teste.local", "senha": "senha-forte-123", "nome": "%s",
                                 "cpf": "%s", "dataNascimento": "1995-01-01", "telefone": "11999990000"}
                                """.formatted("aux" + cpf, nome, cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void listagemDeUsuariosFiltraPorNomePerfilSituacaoEPeriodo() throws Exception {
        String nome = "Paulo Filtro " + sufixo;
        String perfilId = cadastrarAuxiliar(nome);
        String hoje = LocalDate.now().toString();

        mockMvc.perform(get("/api/usuarios").param("nome", "filtro " + sufixo).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value(nome))
                .andExpect(jsonPath("$[0].perfilId").value(perfilId))
                .andExpect(jsonPath("$[0].role").value("ROLE_AUXILIAR"));

        mockMvc.perform(get("/api/usuarios").param("perfil", "ROLE_ADMIN").header("Authorization", admin))
                .andExpect(jsonPath("$[*].role", everyItem(is("ROLE_ADMIN"))))
                .andExpect(jsonPath("$[*].nome", not(hasItem(nome))));

        mockMvc.perform(get("/api/usuarios").param("cadastradoDe", hoje).param("cadastradoAte", hoje)
                        .header("Authorization", admin))
                .andExpect(jsonPath("$[*].nome", hasItem(nome)));

        mockMvc.perform(get("/api/usuarios").param("cadastradoAte", "2000-01-01").header("Authorization", admin))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/auxiliares/{id}/desativar", perfilId).header("Authorization", admin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuarios").param("ativo", "true").header("Authorization", admin))
                .andExpect(jsonPath("$[*].nome", not(hasItem(nome))));
        mockMvc.perform(get("/api/usuarios").param("ativo", "false").header("Authorization", admin))
                .andExpect(jsonPath("$[*].nome", hasItem(nome)));
    }

    @Test
    void listagemDeUsuariosEhSoDoAdmin() throws Exception {
        String nome = "Recepção " + sufixo;
        String cpf = GeradorDeDocumentos.cpf();
        String email = "recep" + cpf + "@teste.local";
        mockMvc.perform(post("/api/recepcionistas")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "senha-forte-123", "nome": "%s",
                                 "cpf": "%s", "dataNascimento": "1995-01-01", "telefone": "11999990000"}
                                """.formatted(email, nome, cpf)))
                .andExpect(status().isCreated());

        String recepcionista = login(email, "senha-forte-123");
        mockMvc.perform(get("/api/usuarios").header("Authorization", recepcionista))
                .andExpect(status().isForbidden());

        // Nomes para os relatórios: qualquer perfil, só e-mail, nome e perfil.
        mockMvc.perform(get("/api/usuarios/nomes").header("Authorization", recepcionista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem(nome)))
                .andExpect(jsonPath("$[*].email", hasItem(email)))
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].ativo").doesNotExist());
        mockMvc.perform(get("/api/usuarios/nomes")).andExpect(status().isUnauthorized());
    }

    @Test
    void putEmVeterinarioTrocaDadosEEmailDeLogin() throws Exception {
        String cpf = GeradorDeDocumentos.cpf();
        int crmv = ThreadLocalRandom.current().nextInt(10_000, 99_999);
        String antigo = "vet" + cpf + "@teste.local";
        String body = mockMvc.perform(post("/api/veterinarios")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "senha-forte-123", "nome": "Dra. Ana", "cpfCnpj": "%s",
                                 "tipoPessoa": "PF", "dataNascimento": "1990-05-10", "telefone": "11999990000",
                                 "crmv": "%d", "crmvUf": "SP"}
                                """.formatted(antigo, cpf, crmv)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        String novo = "novo" + cpf + "@teste.local";

        mockMvc.perform(put("/api/veterinarios/{id}", id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "nome": "Dra. Ana Lima", "cpfCnpj": "%s", "tipoPessoa": "PF",
                                 "dataNascimento": "1990-05-10", "telefone": "11988887777", "crmv": "%d",
                                 "crmvUf": "RJ", "especializacao": "CIRURGIA"}
                                """.formatted(novo, cpf, crmv)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Dra. Ana Lima"))
                .andExpect(jsonPath("$.crmvUf").value("RJ"))
                .andExpect(jsonPath("$.especializacao").value("CIRURGIA"))
                .andExpect(jsonPath("$.usuario.email").value(novo));

        login(novo, "senha-forte-123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "senha-forte-123"}
                                """.formatted(antigo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putEmAuxiliarComCpfDeOutroRecebe409() throws Exception {
        String primeiro = cadastrarAuxiliar("Aux Um " + sufixo);
        String segundo = cadastrarAuxiliar("Aux Dois " + sufixo);
        String cpfDoPrimeiro = JsonPath.read(mockMvc.perform(get("/api/auxiliares/{id}", primeiro)
                .header("Authorization", admin)).andReturn().getResponse().getContentAsString(), "$.cpf");

        mockMvc.perform(put("/api/auxiliares/{id}", segundo)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "aux-dois-%s@teste.local", "nome": "Aux Dois", "cpf": "%s",
                                 "dataNascimento": "1995-01-01", "telefone": "11999990000"}
                                """.formatted(sufixo, cpfDoPrimeiro)))
                .andExpect(status().isConflict());
    }

    @Test
    void putEmAdminTrocaNomeESoOAdminEdita() throws Exception {
        String body = mockMvc.perform(post("/api/admins")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "adm%s@teste.local", "senha": "senha-forte-123", "nome": "Supervisor"}
                                """.formatted(sufixo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(put("/api/admins/{id}", id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "adm%s@teste.local", "nome": "Supervisora TI"}
                                """.formatted(sufixo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Supervisora TI"));

        String recepcao = cadastrarAuxiliar("Sem Permissão " + sufixo);
        String email = JsonPath.read(mockMvc.perform(get("/api/auxiliares/{id}", recepcao)
                .header("Authorization", admin)).andReturn().getResponse().getContentAsString(), "$.usuario.email");
        mockMvc.perform(put("/api/admins/{id}", id)
                        .header("Authorization", login(email, "senha-forte-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"x@teste.local\", \"nome\": \"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void veterinarioLeTutoresEAnimaisMasNaoCadastra() throws Exception {
        String cpf = GeradorDeDocumentos.cpf();
        int crmv = ThreadLocalRandom.current().nextInt(10_000, 99_999);
        String email = "leitor" + cpf + "@teste.local";
        mockMvc.perform(post("/api/veterinarios")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "senha-forte-123", "nome": "Dr. Leitor", "cpfCnpj": "%s",
                                 "tipoPessoa": "PF", "dataNascimento": "1990-05-10", "telefone": "11999990000",
                                 "crmv": "%d", "crmvUf": "SP"}
                                """.formatted(email, cpf, crmv)))
                .andExpect(status().isCreated());
        String vet = login(email, "senha-forte-123");
        long tutor = criarTutor("Tutor Leitura " + sufixo);

        mockMvc.perform(get("/api/tutores").header("Authorization", vet)).andExpect(status().isOk());
        mockMvc.perform(get("/api/tutores/{id}", tutor).header("Authorization", vet)).andExpect(status().isOk());
        mockMvc.perform(get("/api/animais").header("Authorization", vet)).andExpect(status().isOk());

        mockMvc.perform(post("/api/tutores")
                        .header("Authorization", vet)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "X", "endereco": "Rua", "dataNascimento": "1980-01-01", "telefone": "11988887777"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/animais/{id}", criarAnimal(tutor, "Rex", null))
                        .header("Authorization", vet)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAnimal("Rex", null)))
                .andExpect(status().isForbidden());
    }

    // E-mail do tutor é opcional, guardado em minúsculas, e recebe o resumo de alta só no endereço cadastrado.
    @Test
    void tutorGuardaEmailOpcionalERecebeMensagemSoNoEnderecoCadastrado() throws Exception {
        String comEmail = mockMvc.perform(post("/api/tutores").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana %s", "endereco": "Rua A, 10", "dataNascimento": "1980-01-01",
                                 "telefone": "11999990000", "email": "Ana.Tutora@Exemplo.com"}
                                """.formatted(sufixo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana.tutora@exemplo.com"))
                .andReturn().getResponse().getContentAsString();
        long tutor = ((Number) JsonPath.read(comEmail, "$.id")).longValue();

        mockMvc.perform(post("/api/tutores/{id}/mensagens", tutor).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assunto": "Resumo de alta", "texto": "## Atendimento\\n- Thor foi medicado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinatario").value("ana.tutora@exemplo.com"));
        org.mockito.Mockito.verify(emailService).enviarMensagemAoTutor(
                org.mockito.ArgumentMatchers.eq("ana.tutora@exemplo.com"),
                org.mockito.ArgumentMatchers.eq("Resumo de alta"), org.mockito.ArgumentMatchers.contains("Thor"));

        // E-mail inválido: 400. Sem e-mail cadastrado: 422 e nada é enviado.
        mockMvc.perform(post("/api/tutores").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Bia %s", "endereco": "Rua A, 10", "dataNascimento": "1980-01-01",
                                 "telefone": "11999990000", "email": "isso-nao-e-email"}
                                """.formatted(sufixo)))
                .andExpect(status().isBadRequest());
        long semEmail = criarTutor("Caio " + sufixo);
        mockMvc.perform(post("/api/tutores/{id}/mensagens", semEmail).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assunto\": \"x\", \"texto\": \"y\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
