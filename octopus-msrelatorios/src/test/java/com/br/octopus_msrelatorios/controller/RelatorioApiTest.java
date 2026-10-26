package com.br.octopus_msrelatorios.controller;

import com.br.octopus_msrelatorios.client.BaiaClient;
import com.br.octopus_msrelatorios.client.DoseClient;
import com.br.octopus_msrelatorios.client.InternacaoClient;
import com.br.octopus_msrelatorios.client.MedicacaoClient;
import com.br.octopus_msrelatorios.client.PrescricaoClient;
import com.br.octopus_msrelatorios.client.dto.InternacaoEventoDto;
import com.br.octopus_msrelatorios.client.dto.PrescricaoDto;
import com.br.octopus_msrelatorios.exception.RecursoNaoEncontradoException;
import com.br.octopus_msrelatorios.client.UsuarioClient;
import com.br.octopus_msrelatorios.client.dto.BaiaDto;
import com.br.octopus_msrelatorios.client.dto.DoseDto;
import com.br.octopus_msrelatorios.client.dto.InternacaoDto;
import com.br.octopus_msrelatorios.client.dto.MedicacaoDto;
import com.br.octopus_msrelatorios.client.dto.TutorDto;
import com.br.octopus_msrelatorios.client.dto.UsuarioDto;
import com.br.octopus_msrelatorios.client.dto.UsuarioNomeDto;
import com.br.octopus_msrelatorios.exception.ServicoIndisponivelException;
import com.br.octopus_msrelatorios.security.TokensDeTeste;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RelatorioApiTest {

    // 12/10/2026 10:00 na clínica.
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 12, 10, 0);

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioDeTeste() {
            ZoneId fuso = ZoneId.of("America/Sao_Paulo");
            return Clock.fixed(AGORA.atZone(fuso).toInstant(), fuso);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioClient usuarioClient;
    @MockitoBean
    private BaiaClient baiaClient;
    @MockitoBean
    private InternacaoClient internacaoClient;
    @MockitoBean
    private MedicacaoClient medicacaoClient;
    @MockitoBean
    private DoseClient doseClient;
    @MockitoBean
    private PrescricaoClient prescricaoClient;

    private static final String ADMIN = TokensDeTeste.bearer("admin@vidapet.com", "ROLE_ADMIN");
    private static final String VET = TokensDeTeste.bearer("rodrigo@vidapet.com", "ROLE_VETERINARIO");
    private static final String AUXILIAR = TokensDeTeste.bearer("paulo@vidapet.com", "ROLE_AUXILIAR");

    @BeforeEach
    void dados() {
        when(usuarioClient.nomes()).thenReturn(List.of(
                new UsuarioNomeDto("admin@vidapet.com", "Administrador"),
                new UsuarioNomeDto("rodrigo@vidapet.com", "Dr. Rodrigo Alves"),
                new UsuarioNomeDto("paulo@vidapet.com", "Paulo Souza"),
                new UsuarioNomeDto("recepcao@vidapet.com", "Ana Recepção")));
        when(usuarioClient.tutores()).thenReturn(List.of(new TutorDto(1L, "Marina Costa", "Rua das Flores, 10",
                LocalDate.of(1988, 4, 2), "11988887777", "marina@exemplo.com", List.of(
                new TutorDto.AnimalDto(10L, "Thor", 1L, "Cão", LocalDate.of(2025, 10, 3), null),
                new TutorDto.AnimalDto(11L, "Nina", 1L, "Gato", LocalDate.of(2026, 2, 15), null)))));
        when(usuarioClient.usuarios(any(), any())).thenReturn(List.of(
                new UsuarioDto("Dr. Rodrigo Alves", "rodrigo@vidapet.com", "ROLE_VETERINARIO", true,
                        LocalDateTime.of(2026, 9, 1, 8, 0)),
                new UsuarioDto("Paulo Souza", "paulo@vidapet.com", "ROLE_AUXILIAR", false,
                        LocalDateTime.of(2026, 9, 2, 8, 0))));
        when(baiaClient.listar()).thenReturn(List.of(
                new BaiaDto(2L, "COLETIVA", "B-02", null, 1, true),
                new BaiaDto(9L, "ISOLAMENTO", "B-09", null, 1, true)));
        when(internacaoClient.listar()).thenReturn(List.of(
                new InternacaoDto(5L, 10L, "Thor", "Cão", 9L, "EM_TRATAMENTO", "Gastroenterite",
                        LocalDateTime.of(2026, 10, 11, 9, 0), null, null, "recepcao@vidapet.com")));
        when(medicacaoClient.listar()).thenReturn(List.of(
                new MedicacaoDto(1L, "Antibiótico A", "500 mg", "CONTINUO")));
        when(doseClient.listar(any(), any())).thenReturn(List.of(
                // Manhã: aplicada; perdida (8h, já passou da janela); atrasada (9h40, ainda na janela).
                new DoseDto(1L, 1L, 5L, 1L, LocalDateTime.of(2026, 10, 12, 6, 0), "APLICADA",
                        LocalDateTime.of(2026, 10, 12, 6, 10), "paulo@vidapet.com", "Sem intercorrências", null, null),
                new DoseDto(2L, 1L, 5L, 1L, LocalDateTime.of(2026, 10, 12, 8, 0), "PENDENTE", null, null, null, null, null),
                new DoseDto(3L, 1L, 5L, 1L, LocalDateTime.of(2026, 10, 12, 9, 40), "PENDENTE", null, null, null, null, null),
                // Noite: prevista.
                new DoseDto(4L, 1L, 5L, 1L, LocalDateTime.of(2026, 10, 12, 22, 0), "PENDENTE", null, null, null, null, null)));
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/api/relatorios/doses")).andExpect(status().isUnauthorized());
    }

    @Test
    void relatorioDeDosesEmPdfTemLogoNomesESituacoes() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/relatorios/doses").header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("relatorio-doses-2026-10-12.pdf")))
                .andReturn();

        String texto = textoDoPdf(resultado.getResponse().getContentAsByteArray());
        assertThat(texto)
                .contains("VidaPet", "Relatório de doses", "Período: 12/10/2026")
                .contains("Paulo Souza", "Thor", "B-09", "Antibiótico A")
                .contains("Aplicada", "Perdida", "Atrasada", "Prevista")
                .contains("Emitido por Paulo Souza (Auxiliar)")
                .doesNotContain("paulo@vidapet.com (Auxiliar)")
                // O total de páginas é escrito depois, num template; o extrator o devolve em outra linha.
                .containsPattern("Página 1 de\\s*1");
    }

    @Test
    void filtroPorTurnoESituacao() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/relatorios/doses")
                        .param("formato", "XLSX").param("turno", "MANHA").param("situacao", "PERDIDA")
                        .header("Authorization", VET))
                .andExpect(status().isOk())
                .andReturn();

        try (XSSFWorkbook planilha = planilha(resultado)) {
            XSSFSheet doses = planilha.getSheet("Doses");
            assertThat(doses.getLastRowNum()).isEqualTo(6);
            assertThat(doses.getRow(6).getCell(5).getStringCellValue()).isEqualTo("Perdida");
        }
    }

    @Test
    void relatorioDeDosesEmXlsxTemUmaAbaPorTabelaEDatasComoData() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/relatorios/doses").param("formato", "XLSX")
                        .header("Authorization", ADMIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("relatorio-doses-2026-10-12.xlsx")))
                .andReturn();

        try (XSSFWorkbook planilha = planilha(resultado)) {
            assertThat(planilha.getNumberOfSheets()).isEqualTo(3);
            assertThat(planilha.getSheetName(0)).isEqualTo("Por turno");
            XSSFSheet doses = planilha.getSheet("Doses");
            assertThat(doses.getRow(0).getCell(0).getStringCellValue()).startsWith("VidaPet");
            assertThat(doses.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Horário previsto");
            var horario = doses.getRow(6).getCell(0);
            assertThat(horario.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(DateUtil.isCellDateFormatted(horario)).isTrue();
            assertThat(horario.getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026, 10, 12, 6, 0));
            assertThat(doses.getRow(6).getCell(7).getStringCellValue()).isEqualTo("Paulo Souza");

            XSSFSheet porTurno = planilha.getSheet("Por turno");
            // Manhã: 1 aplicada, 1 atrasada, 1 perdida, 0 prevista, 0 cancelada, total 3.
            var manha = porTurno.getRow(6);
            assertThat(manha.getCell(0).getStringCellValue()).startsWith("Manhã");
            assertThat(manha.getCell(1).getNumericCellValue()).isEqualTo(1);
            assertThat(manha.getCell(2).getNumericCellValue()).isEqualTo(1);
            assertThat(manha.getCell(3).getNumericCellValue()).isEqualTo(1);
            assertThat(manha.getCell(6).getNumericCellValue()).isEqualTo(3);
            assertThat(porTurno.getRow(8).getCell(4).getNumericCellValue()).isEqualTo(1);
        }
    }

    @Test
    void periodoInvertidoRecebe400() throws Exception {
        mockMvc.perform(get("/api/relatorios/doses").param("de", "2026-10-12").param("ate", "2026-10-01")
                        .header("Authorization", ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O início do período não pode ser depois do fim."));
    }

    @Test
    void formatoInexistenteRecebe400() throws Exception {
        mockMvc.perform(get("/api/relatorios/baias").param("formato", "DOCX").header("Authorization", ADMIN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void auxiliarNaoGeraRelatorioDeInternacoesNemDeTutores() throws Exception {
        mockMvc.perform(get("/api/relatorios/internacoes").header("Authorization", AUXILIAR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/relatorios/tutores-animais").header("Authorization", AUXILIAR))
                .andExpect(status().isForbidden());
    }

    @Test
    void internacoesTrazemTutorBaiaEQuemRegistrou() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/relatorios/internacoes").header("Authorization", VET))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(textoDoPdf(resultado.getResponse().getContentAsByteArray()))
                .contains("Relatório de internações", "Thor", "Marina Costa", "11988887777", "B-09",
                        "Em tratamento", "Ana Recepção", "Emitido por Dr. Rodrigo Alves (Veterinário)");
    }

    @Test
    void statusDeInternacaoInexistenteRecebe400() throws Exception {
        mockMvc.perform(get("/api/relatorios/internacoes").param("status", "SUMIDA").header("Authorization", ADMIN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tutoresEAnimaisMostramAntirrabicaVencida() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/relatorios/tutores-animais").param("formato", "XLSX")
                        .header("Authorization", ADMIN))
                .andExpect(status().isOk())
                .andReturn();

        try (XSSFWorkbook planilha = planilha(resultado)) {
            XSSFSheet animais = planilha.getSheet("Animais");
            assertThat(animais.getRow(6).getCell(0).getStringCellValue()).isEqualTo("Nina");
            assertThat(animais.getRow(6).getCell(5).getStringCellValue()).isEqualTo("Em dia");
            assertThat(animais.getRow(7).getCell(0).getStringCellValue()).isEqualTo("Thor");
            assertThat(animais.getRow(7).getCell(5).getStringCellValue()).isEqualTo("Vencida");
        }
    }

    @Test
    void ocupacaoDeBaiasParaQualquerPerfil() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/relatorios/baias").header("Authorization", AUXILIAR))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(textoDoPdf(resultado.getResponse().getContentAsByteArray()))
                .contains("Relatório de ocupação de baias", "B-02", "B-09", "Thor (em tratamento)");
    }

    @Test
    void usuariosSoParaAdmin() throws Exception {
        mockMvc.perform(get("/api/relatorios/usuarios").header("Authorization", VET))
                .andExpect(status().isForbidden());

        MvcResult resultado = mockMvc.perform(get("/api/relatorios/usuarios").header("Authorization", ADMIN))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(textoDoPdf(resultado.getResponse().getContentAsByteArray()))
                .contains("Relatório de usuários", "Dr. Rodrigo Alves", "Veterinário", "Desativado");
    }

    @Test
    void moduloForaDoArRecebe503() throws Exception {
        when(doseClient.listar(any(), any())).thenThrow(new ServicoIndisponivelException("octopus-msdoses não respondeu."));

        mockMvc.perform(get("/api/relatorios/doses").header("Authorization", ADMIN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value("octopus-msdoses não respondeu."));
    }

    @Test
    void prescricaoSaiEmPdfComPacienteTutorEAssinaturaDoVeterinario() throws Exception {
        when(prescricaoClient.buscar(7L)).thenReturn(new PrescricaoDto(7L, 5L, "rodrigo@vidapet.com",
                "Manter em repouso", true, LocalDateTime.of(2026, 10, 11, 10, 0), List.of(
                new PrescricaoDto.ItemDto(1L, 1L, "1 comprimido", false, 8, 6,
                        LocalDateTime.of(2026, 10, 11, 14, 0), null))));

        MvcResult resultado = mockMvc.perform(get("/api/relatorios/prescricoes/7").header("Authorization", VET))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("prescricao-7")))
                .andReturn();

        String texto = textoDoPdf(resultado.getResponse().getContentAsByteArray());
        assertThat(texto).contains("Prescrição médica", "Thor", "Marina Costa", "(11) 98888-7777",
                "Antibiótico A 500 mg", "A cada 8 h", "6 doses", "Manter em repouso",
                "Dr. Rodrigo Alves", "ASSINADO DIGITALMENTE")
                .doesNotContain("Dr. Dr.");
    }

    @Test
    void prescricaoSoParaVeterinarioEAdmin() throws Exception {
        when(prescricaoClient.buscar(7L)).thenReturn(new PrescricaoDto(7L, 5L, "rodrigo@vidapet.com", null, true,
                LocalDateTime.of(2026, 10, 11, 10, 0), List.of()));

        mockMvc.perform(get("/api/relatorios/prescricoes/7")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/relatorios/prescricoes/7").header("Authorization", AUXILIAR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/relatorios/prescricoes/7").header("Authorization", ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void prescricaoQueNaoExisteRecebe404() throws Exception {
        when(prescricaoClient.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Prescrição não encontrada: 99"));

        mockMvc.perform(get("/api/relatorios/prescricoes/99").header("Authorization", VET))
                .andExpect(status().isNotFound());
    }

    /** Thor (animal 10, tutora Marina) internado na 6 e com alta autorizada; atendido por Rodrigo e Paulo. */
    private void internacaoComAlta(String statusDaAlta) {
        when(usuarioClient.nomes()).thenReturn(List.of(
                new UsuarioNomeDto("recepcao@vidapet.com", "Ana Recepção", "ROLE_RECEPCIONISTA"),
                new UsuarioNomeDto("rodrigo@vidapet.com", "Dr. Rodrigo Alves", "ROLE_VETERINARIO"),
                new UsuarioNomeDto("paulo@vidapet.com", "Paulo Souza", "ROLE_AUXILIAR"),
                new UsuarioNomeDto("admin@vidapet.com", "Administrador", "ROLE_ADMIN")));
        when(internacaoClient.listar()).thenReturn(List.of(
                new InternacaoDto(6L, 10L, "Thor", "Cão", 9L, statusDaAlta, "Gastroenterite",
                        LocalDateTime.of(2026, 10, 9, 9, 0), LocalDateTime.of(2026, 10, 12, 9, 30), null,
                        "recepcao@vidapet.com")));
        when(internacaoClient.eventos(6L)).thenReturn(List.of(
                new InternacaoEventoDto(null, "ADMITIDA", 9L, "recepcao@vidapet.com", LocalDateTime.of(2026, 10, 9, 9, 0)),
                new InternacaoEventoDto("ADMITIDA", "EM_TRATAMENTO", 9L, "rodrigo@vidapet.com", LocalDateTime.of(2026, 10, 9, 10, 0)),
                new InternacaoEventoDto("EM_TRATAMENTO", statusDaAlta, 9L, "rodrigo@vidapet.com", LocalDateTime.of(2026, 10, 12, 9, 30))));
        when(prescricaoClient.listarDaInternacao(6L)).thenReturn(List.of(
                new PrescricaoDto(7L, 6L, "rodrigo@vidapet.com", "Manter em repouso", true,
                        LocalDateTime.of(2026, 10, 9, 10, 15), List.of(
                        new PrescricaoDto.ItemDto(1L, 1L, "1 comprimido", false, 8, 6,
                                LocalDateTime.of(2026, 10, 9, 14, 0), null))),
                new PrescricaoDto(8L, 6L, "rodrigo@vidapet.com", null, false,
                        LocalDateTime.of(2026, 10, 9, 10, 20), List.of(
                        new PrescricaoDto.ItemDto(2L, 1L, "2 gotas", false, 12, 4,
                                LocalDateTime.of(2026, 10, 9, 14, 0), null)))));
        when(doseClient.listarDaInternacao(6L)).thenReturn(List.of(
                new DoseDto(1L, 7L, 6L, 1L, LocalDateTime.of(2026, 10, 9, 14, 0), "APLICADA",
                        LocalDateTime.of(2026, 10, 9, 14, 5), "paulo@vidapet.com", null, null, null),
                new DoseDto(2L, 7L, 6L, 1L, LocalDateTime.of(2026, 10, 9, 22, 0), "APLICADA",
                        LocalDateTime.of(2026, 10, 9, 22, 10), "paulo@vidapet.com", null, null, null),
                new DoseDto(3L, 7L, 6L, 1L, LocalDateTime.of(2026, 10, 10, 6, 0), "PERDIDA", null, null, null, null, null)));
    }

    @Test
    void enviaResumoDeAltaAoEmailDoTutorComAtendimentoEquipeMedicacoesECuidados() throws Exception {
        internacaoComAlta("ALTA_AUTORIZADA");
        when(usuarioClient.enviarMensagemAoTutor(any(), any(), any())).thenReturn("marina@exemplo.com");

        mockMvc.perform(post("/api/relatorios/altas/6/email").header("Authorization", VET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinatario").value("marina@exemplo.com"));

        org.mockito.ArgumentCaptor<String> assunto = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> texto = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(usuarioClient).enviarMensagemAoTutor(org.mockito.ArgumentMatchers.eq(1L),
                assunto.capture(), texto.capture());
        assertThat(assunto.getValue()).isEqualTo("Resumo de alta de Thor - Plantão VidaPet");
        assertThat(texto.getValue()).contains("Olá, Marina Costa.", "Thor (Cão)", "Gastroenterite",
                "alta autorizada pelo veterinário",
                "Ana Recepção (Recepcionista): registrou a admissão",
                "Dr(a). Rodrigo Alves (Veterinário): iniciou o tratamento; autorizou a alta; prescreveu 1 medicamento",
                "Paulo Souza (Auxiliar): aplicou 2 doses",
                "Antibiótico A 500 mg, 1 comprimido, a cada 8 h: 2 doses aplicadas",
                "1 dose não foi aplicada no horário",
                "Manter em repouso", "## Cuidados para os próximos 7 dias", "## Procure a clínica na hora se")
                .doesNotContain("2 gotas"); // prescrição desativada sem nenhuma aplicação não entra
    }

    @Test
    void resumoDeAltaUsaOsCuidadosDoGatoParaGatoEAltaAPedidoDoTutor() throws Exception {
        internacaoComAlta("ALTA_A_PEDIDO_DO_TUTOR");
        when(internacaoClient.listar()).thenReturn(List.of(
                new InternacaoDto(6L, 11L, "Nina", "Gato", 9L, "ALTA_A_PEDIDO_DO_TUTOR", "Cirurgia",
                        LocalDateTime.of(2026, 10, 9, 9, 0), LocalDateTime.of(2026, 10, 12, 9, 30), null,
                        "recepcao@vidapet.com")));
        when(usuarioClient.enviarMensagemAoTutor(any(), any(), any())).thenReturn("marina@exemplo.com");

        mockMvc.perform(post("/api/relatorios/altas/6/email").header("Authorization", ADMIN)).andExpect(status().isOk());

        org.mockito.ArgumentCaptor<String> texto = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(usuarioClient).enviarMensagemAoTutor(any(), any(), texto.capture());
        assertThat(texto.getValue()).contains("alta a pedido do tutor", "caixa de areia");
    }

    @Test
    void tutorSemEmailRecebe422NoEnvioEOPdfSaiParaImprimir() throws Exception {
        internacaoComAlta("ALTA_AUTORIZADA");
        when(usuarioClient.tutores()).thenReturn(List.of(new TutorDto(1L, "Marina Costa", "Rua das Flores, 10",
                LocalDate.of(1988, 4, 2), "11988887777", null, List.of(
                new TutorDto.AnimalDto(10L, "Thor", 1L, "Cão", LocalDate.of(2025, 10, 3), null)))));

        mockMvc.perform(post("/api/relatorios/altas/6/email").header("Authorization", VET))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("não tem e-mail")));
        org.mockito.Mockito.verify(usuarioClient, org.mockito.Mockito.never()).enviarMensagemAoTutor(any(), any(), any());

        MvcResult pdf = mockMvc.perform(get("/api/relatorios/altas/6").header("Authorization", VET))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();
        assertThat(textoDoPdf(pdf.getResponse().getContentAsByteArray())).contains("Resumo de alta", "Thor",
                "Marina Costa", "Paulo Souza", "Antibiótico A 500 mg", "Procure a clínica na hora se");
    }

    @Test
    void servidorDeEmailRecusandoVira502ComMensagem() throws Exception {
        internacaoComAlta("ALTA_AUTORIZADA");
        when(usuarioClient.enviarMensagemAoTutor(any(), any(), any()))
                .thenThrow(new com.br.octopus_msrelatorios.exception.EmailNaoEnviadoException(
                        "O servidor de e-mail não aceitou o envio."));

        mockMvc.perform(post("/api/relatorios/altas/6/email").header("Authorization", VET))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("servidor de e-mail")));
    }

    @Test
    void resumoDeAltaSoDepoisDaAltaESoParaQuemAtende() throws Exception {
        // A internação 5 (EM_TRATAMENTO, do @BeforeEach) ainda não teve alta.
        mockMvc.perform(post("/api/relatorios/altas/5/email").header("Authorization", VET))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/relatorios/altas/5").header("Authorization", VET))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/relatorios/altas/99").header("Authorization", VET))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/relatorios/altas/5").header("Authorization", AUXILIAR))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/relatorios/altas/5/email").header("Authorization", AUXILIAR))
                .andExpect(status().isForbidden());
    }

    private static String textoDoPdf(byte[] pdf) throws Exception {
        PdfReader leitor = new PdfReader(pdf);
        try {
            PdfTextExtractor extrator = new PdfTextExtractor(leitor);
            StringBuilder texto = new StringBuilder();
            for (int pagina = 1; pagina <= leitor.getNumberOfPages(); pagina++) {
                texto.append(extrator.getTextFromPage(pagina)).append('\n');
            }
            return texto.toString();
        } finally {
            leitor.close();
        }
    }

    private static XSSFWorkbook planilha(MvcResult resultado) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()));
    }
}
