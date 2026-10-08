package com.br.octopus_msinternacao.controller;

import com.br.octopus_msinternacao.client.AnimalClient;
import com.br.octopus_msinternacao.client.BaiaClient;
import com.br.octopus_msinternacao.client.dto.AnimalDto;
import com.br.octopus_msinternacao.client.dto.BaiaDto;
import com.br.octopus_msinternacao.client.dto.TipoBaia;
import com.br.octopus_msinternacao.dto.request.AdmissaoRequest;
import com.br.octopus_msinternacao.dto.request.EncerramentoRequest;
import com.br.octopus_msinternacao.exception.ServicoIndisponivelException;
import com.br.octopus_msinternacao.repository.InternacaoRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// API inteira (segurança, validação, service e H2) com os outros módulos mockados.
@SpringBootTest
@AutoConfigureMockMvc
class InternacaoApiTest {

    private static final String URL = "/api/internacoes";
    private static final String RECEPCAO = "recepcao@vidapet.local";
    private static final String VET = "vet@vidapet.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InternacaoRepository repository;

    @Autowired
    private Clock clock;

    @MockitoBean
    private AnimalClient animalClient;

    @MockitoBean
    private BaiaClient baiaClient;

    @Value("${app.security.jwt.secret}")
    private String segredoBase64;

    private AnimalDto animal;
    private BaiaDto baia;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        animal = new AnimalDto(UUID.randomUUID(), "Thor", "Cão", LocalDate.now(clock).minusMonths(3), null);
        baia = new BaiaDto(UUID.randomUUID(), TipoBaia.COLETIVA, "B-01", 1, true);
        when(animalClient.buscar(animal.id())).thenReturn(animal);
        when(baiaClient.buscar(baia.id())).thenReturn(baia);
    }

    // Mesmo formato do token do octopus-msusuario: subject = e-mail, claim "role".
    private String token(String email, String role) {
        var chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredoBase64));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(chave)
                .compact();
    }

    private ResultActions comoRecepcao(MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(RECEPCAO, "ROLE_RECEPCIONISTA")));
    }

    private ResultActions comoVet(MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(VET, "ROLE_VETERINARIO")));
    }

    private ResultActions como(String role, MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token("x@vidapet.local", role)));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, Object corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(corpo));
    }

    private MockHttpServletRequestBuilder admissao() {
        return json(post(URL), new AdmissaoRequest(animal.id(), baia.id(), "Gastroenterite"));
    }

    private String admitir() throws Exception {
        String corpo = comoRecepcao(admissao())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asString();
    }

    // ---------- segurança ----------

    @Test
    @DisplayName("sem token: 401")
    void semToken() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("auxiliar consulta o painel, mas não admite: 200 e 403")
    void auxiliarSoLe() throws Exception {
        como("ROLE_AUXILIAR", get(URL)).andExpect(status().isOk());
        como("ROLE_AUXILIAR", admissao()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("veterinário não admite: 403")
    void veterinarioNaoAdmite() throws Exception {
        comoVet(admissao()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("recepcionista não autoriza alta nem isola: 403")
    void recepcionistaNaoFazAtoClinico() throws Exception {
        String id = admitir();
        comoRecepcao(patch(URL + "/" + id + "/iniciar-tratamento")).andExpect(status().isForbidden());
        comoRecepcao(patch(URL + "/" + id + "/autorizar-alta")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin admite, mas não faz ato clínico: 201 e 403")
    void adminNaoFazAtoClinico() throws Exception {
        String corpo = como("ROLE_ADMIN", admissao()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(corpo).get("id").asString();

        como("ROLE_ADMIN", patch(URL + "/" + id + "/iniciar-tratamento")).andExpect(status().isForbidden());
    }

    // ---------- fluxo completo ----------

    @Test
    @DisplayName("fluxo completo: admissão → tratamento → alta → saída física, com quem fez cada passo no histórico")
    void fluxoCompleto() throws Exception {
        String id = admitir();

        comoVet(patch(URL + "/" + id + "/iniciar-tratamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_TRATAMENTO"))
                .andExpect(jsonPath("$.animalNome").value("Thor"));
        comoVet(patch(URL + "/" + id + "/autorizar-alta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALTA_AUTORIZADA"));

        // RN-08: enquanto não houver saída física a baia continua ocupada, então outro animal não entra.
        AnimalDto outro = new AnimalDto(UUID.randomUUID(), "Nina", "Gato", LocalDate.now(clock), null);
        when(animalClient.buscar(outro.id())).thenReturn(outro);
        comoRecepcao(json(post(URL), new AdmissaoRequest(outro.id(), baia.id(), "Fratura")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("RN-01")));

        comoRecepcao(json(patch(URL + "/" + id + "/encerrar"), new EncerramentoRequest(LocalDateTime.now(clock))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"));

        comoVet(get(URL + "/" + id + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].statusNovo").value("ADMITIDA"))
                .andExpect(jsonPath("$[0].usuario").value(RECEPCAO))
                .andExpect(jsonPath("$[2].statusNovo").value("ALTA_AUTORIZADA"))
                .andExpect(jsonPath("$[2].usuario").value(VET))
                .andExpect(jsonPath("$[3].statusNovo").value("ENCERRADA"));

        // Com a saída registrada a vaga foi liberada.
        comoRecepcao(json(post(URL), new AdmissaoRequest(outro.id(), baia.id(), "Fratura")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("filtro por status na listagem")
    void listarPorStatus() throws Exception {
        admitir();

        comoVet(get(URL).param("status", "ADMITIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        comoVet(get(URL).param("status", "ENCERRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- erros ----------

    @Test
    @DisplayName("RN-02 pela API: 422 com a mensagem da regra")
    void rn02PelaApi() throws Exception {
        AnimalDto semVacina = new AnimalDto(UUID.randomUUID(), "Bidu", "Cão", null, null);
        when(animalClient.buscar(semVacina.id())).thenReturn(semVacina);

        comoRecepcao(json(post(URL), new AdmissaoRequest(semVacina.id(), baia.id(), "Tosse")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("RN-02")));
    }

    @Test
    @DisplayName("mesmo animal admitido duas vezes: 409")
    void animalDuasVezes() throws Exception {
        admitir();
        comoRecepcao(admissao()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("transição inválida (alta sem tratamento): 422")
    void transicaoInvalida() throws Exception {
        String id = admitir();
        comoVet(patch(URL + "/" + id + "/autorizar-alta")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("corpo sem motivo: 400 apontando o campo")
    void corpoInvalido() throws Exception {
        comoRecepcao(json(post(URL), new AdmissaoRequest(animal.id(), baia.id(), "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.motivo").exists());
    }

    @Test
    @DisplayName("JSON malformado: 400, não 500")
    void jsonMalformado() throws Exception {
        comoRecepcao(post(URL).contentType(MediaType.APPLICATION_JSON).content("{ isso não é json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("id que não é UUID e status inexistente: 400, não 500")
    void parametrosInvalidos() throws Exception {
        comoVet(get(URL + "/123")).andExpect(status().isBadRequest());
        comoVet(get(URL).param("status", "XPTO")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("internação inexistente: 404")
    void inexistente() throws Exception {
        comoVet(get(URL + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("msbaias fora do ar: 503")
    void servicoIndisponivel() throws Exception {
        when(baiaClient.buscar(baia.id())).thenThrow(new ServicoIndisponivelException("msbaias não respondeu."));

        comoRecepcao(admissao())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value("msbaias não respondeu."));
    }
}
