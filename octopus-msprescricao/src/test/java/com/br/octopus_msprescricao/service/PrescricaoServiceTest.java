package com.br.octopus_msprescricao.service;

import com.br.octopus_msprescricao.client.DoseClient;
import com.br.octopus_msprescricao.client.InternacaoClient;
import com.br.octopus_msprescricao.client.MedicacaoClient;
import com.br.octopus_msprescricao.client.dto.AgendamentoDosesDto;
import com.br.octopus_msprescricao.client.dto.DoseDto;
import com.br.octopus_msprescricao.client.dto.SituacaoDosesDto;
import com.br.octopus_msprescricao.client.dto.InternacaoDto;
import com.br.octopus_msprescricao.client.dto.MedicacaoDto;
import com.br.octopus_msprescricao.domain.ItemPrescricao;
import com.br.octopus_msprescricao.domain.Prescricao;
import com.br.octopus_msprescricao.dto.request.ItemPrescricaoRequest;
import com.br.octopus_msprescricao.dto.request.PrescricaoRequest;
import com.br.octopus_msprescricao.dto.response.PrescricaoResponse;
import com.br.octopus_msprescricao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msprescricao.exception.RegraDeNegocioException;
import com.br.octopus_msprescricao.exception.ServicoIndisponivelException;
import com.br.octopus_msprescricao.repository.PrescricaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Prescrição com os outros módulos mockados e o relógio fixo em 09/10/2026 10:00 (São Paulo).
@ExtendWith(MockitoExtension.class)
class PrescricaoServiceTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 9, 10, 0);
    private static final LocalDateTime AS_8H = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final LocalDateTime AS_14H = LocalDateTime.of(2026, 10, 10, 14, 0);
    private static final Long INTERNACAO = 7L;
    // Ids de medicamento sequenciais: nunca repetem e não colidem com INTERNACAO.
    private static final AtomicLong PROXIMO_MEDICAMENTO = new AtomicLong(100);

    @Mock
    private PrescricaoRepository prescricaoRepository;
    @Mock
    private InternacaoClient internacaoClient;
    @Mock
    private MedicacaoClient medicacaoClient;
    @Mock
    private DoseClient doseClient;
    @Mock
    private TransactionTemplate transacao;

    private PrescricaoService prescricaoService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AGORA.atZone(SAO_PAULO).toInstant(), SAO_PAULO);
        prescricaoService = new PrescricaoService(prescricaoRepository, internacaoClient, medicacaoClient, doseClient,
                transacao, clock, java.time.LocalDate.of(2026, 11, 1));
        // O TransactionTemplate mockado só executa o bloco, como faria o de verdade sem banco.
        lenient().when(transacao.execute(any()))
                .thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        lenient().doAnswer(inv -> {
            inv.<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>getArgument(0).accept(null);
            return null;
        }).when(transacao).executeWithoutResult(any());
        lenient().when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));
        // Por padrão, todo medicamento existe e não tem interação proibida.
        lenient().when(medicacaoClient.buscar(anyLong()))
                .thenAnswer(inv -> new MedicacaoDto(inv.getArgument(0), "Medicamento", true, 1000, "CONTINUO", List.of()));
    }

    private static Long novoId() {
        return PROXIMO_MEDICAMENTO.incrementAndGet();
    }

    private void internacaoEm(String status) {
        when(internacaoClient.buscar(INTERNACAO)).thenReturn(new InternacaoDto(INTERNACAO, status, "Thor"));
    }

    private ItemPrescricaoRequest item(int intervaloHoras, int quantidadeDoses) {
        return item(novoId(), intervaloHoras, quantidadeDoses, AS_8H);
    }

    private ItemPrescricaoRequest item(Long medicacaoId, int intervaloHoras, int quantidadeDoses, LocalDateTime inicio) {
        return new ItemPrescricaoRequest(medicacaoId, "5 mg", null, intervaloHoras, quantidadeDoses, inicio, null);
    }

    /** Item de uma prescrição ativa já gravada na internação. */
    private ItemPrescricao emUso(Long medicacaoId, int intervaloHoras, int quantidadeDoses, LocalDateTime inicio) {
        return ItemPrescricao.builder().medicacaoId(medicacaoId).dosagem("5 mg").intervaloHoras(intervaloHoras)
                .quantidadeDoses(quantidadeDoses).inicio(inicio).build();
    }

    /** Cadastra no mock do msmedications um par com interação proibida, gravado nos dois sentidos. */
    private void interacaoProibida(Long a, String nomeA, Long b, String nomeB) {
        lenient().when(medicacaoClient.buscar(a)).thenReturn(
                new MedicacaoDto(a, nomeA, true, 1000, "CONTINUO", List.of(new MedicacaoDto.Interacao(b, nomeB))));
        lenient().when(medicacaoClient.buscar(b)).thenReturn(
                new MedicacaoDto(b, nomeB, true, 1000, "CONTINUO", List.of(new MedicacaoDto.Interacao(a, nomeA))));
    }

    private PrescricaoRequest pedido(ItemPrescricaoRequest... itens) {
        return new PrescricaoRequest(INTERNACAO, "Animal agitado", List.of(itens));
    }

    @Test
    void criar_deveGravarOEmailDoTokenAHoraDaClinicaELigarOsItens() {
        internacaoEm("ADMITIDA");

        PrescricaoResponse resposta = prescricaoService.criar(pedido(item(8, 3), item(12, 2)), "vet@teste.local");

        ArgumentCaptor<Prescricao> captor = ArgumentCaptor.forClass(Prescricao.class);
        verify(prescricaoRepository).save(captor.capture());
        Prescricao salva = captor.getValue();
        assertThat(salva.getVeterinarioEmail()).isEqualTo("vet@teste.local");
        assertThat(salva.getInternacaoId()).isEqualTo(INTERNACAO);
        assertThat(salva.getCriadoEm()).isEqualTo(AGORA);
        assertThat(salva.getItens()).hasSize(2).allSatisfy(i -> assertThat(i.getPrescricao()).isSameAs(salva));
        assertThat(resposta.itens()).extracting("intervaloHoras").containsExactly(8, 12);
    }

    @Test
    void criar_deveGerarOsHorariosAPartirDoInicioDoIntervaloEDaQuantidade() {
        internacaoEm("ADMITIDA");

        PrescricaoResponse resposta = prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local");

        assertThat(resposta.itens().get(0).horarios())
                .containsExactly(AS_8H, AS_8H.plusHours(8), AS_8H.plusHours(16));
    }

    @Test
    void criar_deveAgendarUmaDosePorHorarioNoMsdosesDepoisDeGravar() {
        internacaoEm("ADMITIDA");

        prescricaoService.criar(pedido(item(12, 2)), "vet@teste.local");

        InOrder ordem = inOrder(prescricaoRepository, doseClient);
        ordem.verify(prescricaoRepository).save(any(Prescricao.class));
        ArgumentCaptor<AgendamentoDosesDto> captor = ArgumentCaptor.forClass(AgendamentoDosesDto.class);
        ordem.verify(doseClient).agendar(captor.capture());
        assertThat(captor.getValue().internacaoId()).isEqualTo(INTERNACAO);
        assertThat(captor.getValue().itens()).singleElement()
                .satisfies(i -> assertThat(i.horarios()).containsExactly(AS_8H, AS_8H.plusHours(12)));
    }

    @Test
    void criar_falhaNoMsdosesDesativaAPrescricaoECancelaAsDosesQuePossamTerSidoCriadas() {
        internacaoEm("ADMITIDA");
        Prescricao gravada = Prescricao.builder().id(9L).internacaoId(INTERNACAO)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA).build();
        when(prescricaoRepository.save(any(Prescricao.class))).thenReturn(gravada);
        when(prescricaoRepository.findById(9L)).thenReturn(Optional.of(gravada));
        doThrow(new ServicoIndisponivelException("octopus-msdoses não respondeu.")).when(doseClient).agendar(any());

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local"))
                .isInstanceOf(ServicoIndisponivelException.class);
        assertThat(gravada.isAtivo()).isFalse();
        verify(doseClient).cancelar(9L);
    }

    @Test
    void criar_falhaTambemNoCancelamentoAindaDevolveOErroOriginal() {
        internacaoEm("ADMITIDA");
        Prescricao gravada = Prescricao.builder().id(9L).internacaoId(INTERNACAO)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA).build();
        when(prescricaoRepository.save(any(Prescricao.class))).thenReturn(gravada);
        when(prescricaoRepository.findById(9L)).thenReturn(Optional.of(gravada));
        doThrow(new ServicoIndisponivelException("octopus-msdoses não respondeu.")).when(doseClient).agendar(any());
        doThrow(new ServicoIndisponivelException("octopus-msdoses não respondeu.")).when(doseClient).cancelar(9L);

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local"))
                .isInstanceOf(ServicoIndisponivelException.class);
        assertThat(gravada.isAtivo()).isFalse();
    }

    @Test
    void criar_semEstoqueParaTodasAsDosesRecusaSemGravarNemIniciarTratamento() {
        internacaoEm("ADMITIDA");
        Long dipirona = novoId();
        when(medicacaoClient.buscar(dipirona)).thenReturn(new MedicacaoDto(dipirona, "Dipirona", true, 4, "CONTINUO", List.of()));

        // Dois itens do mesmo medicamento somam 3 + 2 = 5 doses, uma a mais que o estoque.
        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(dipirona, 8, 3, AS_8H),
                item(dipirona, 12, 2, AS_8H.plusHours(1))), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Estoque insuficiente de Dipirona: 4 em estoque, 5 doses prescritas.");
        verify(prescricaoRepository, never()).save(any());
        verify(internacaoClient, never()).iniciarTratamento(any());
    }

    // O ms-internacao só aceita Admitida → Em tratamento com prescrição ativa: ela precisa estar gravada antes.
    @Test
    void criar_deveIniciarOTratamentoDepoisDeGravarEAgendar() {
        internacaoEm("ADMITIDA");

        prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local");

        InOrder ordem = inOrder(internacaoClient, prescricaoRepository, doseClient);
        ordem.verify(internacaoClient).buscar(INTERNACAO);
        ordem.verify(prescricaoRepository).save(any(Prescricao.class));
        ordem.verify(doseClient).agendar(any());
        ordem.verify(internacaoClient).iniciarTratamento(INTERNACAO);
    }

    @Test
    void criar_falhaAoIniciarOTratamentoDesfazAPrescricaoEAsDoses() {
        internacaoEm("ADMITIDA");
        Prescricao gravada = Prescricao.builder().id(9L).internacaoId(INTERNACAO)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA).build();
        when(prescricaoRepository.save(any(Prescricao.class))).thenReturn(gravada);
        when(prescricaoRepository.findById(9L)).thenReturn(Optional.of(gravada));
        doThrow(new ServicoIndisponivelException("ms-internacao não respondeu."))
                .when(internacaoClient).iniciarTratamento(INTERNACAO);

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local"))
                .isInstanceOf(ServicoIndisponivelException.class);
        assertThat(gravada.isAtivo()).isFalse();
        verify(doseClient).cancelar(9L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EM_TRATAMENTO", "ISOLAMENTO"})
    void criar_deveAceitarInternacaoJaEmTratamento(String status) {
        internacaoEm(status);

        prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local");

        verify(prescricaoRepository).save(any(Prescricao.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALTA_AUTORIZADA", "ALTA_A_PEDIDO_DO_TUTOR", "ENCERRADA"})
    void criar_deveRecusarInternacaoComAltaOuEncerrada(String status) {
        internacaoEm(status);

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining(status);
        verify(internacaoClient, never()).iniciarTratamento(anyLong());
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void criar_naoDeveGravarQuandoAInternacaoNaoExiste() {
        when(internacaoClient.buscar(INTERNACAO))
                .thenThrow(new RecursoNaoEncontradoException("Internação não encontrada: " + INTERNACAO));

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(prescricaoRepository, never()).save(any());
    }

    // Caso de teste 4 do enunciado: Antibiótico A às 14h e Analgésico C às 14h.
    @Test
    void rn06_deveRecusarDoisMedicamentosComInteracaoNoMesmoHorarioNaMesmaPrescricao() {
        internacaoEm("ADMITIDA");
        Long antibiotico = novoId();
        Long analgesico = novoId();
        interacaoProibida(antibiotico, "Antibiótico A", analgesico, "Analgésico C");

        assertThatThrownBy(() -> prescricaoService.criar(
                pedido(item(antibiotico, 8, 3, AS_14H), item(analgesico, 12, 2, AS_14H)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("RN-06")
                .hasMessageContaining("Antibiótico A")
                .hasMessageContaining("Analgésico C")
                .hasMessageContaining("10/10 às 14:00");
        verify(internacaoClient, never()).iniciarTratamento(anyLong());
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void rn06_deveAceitarMedicamentosComInteracaoEmHorariosDiferentes() {
        internacaoEm("ADMITIDA");
        Long antibiotico = novoId();
        Long analgesico = novoId();
        interacaoProibida(antibiotico, "Antibiótico A", analgesico, "Analgésico C");

        // A: 08h, 16h e 00h. C: 12h e 12h do dia seguinte. Nenhum horário em comum.
        prescricaoService.criar(pedido(item(antibiotico, 8, 3, AS_8H), item(analgesico, 24, 2, AS_8H.plusHours(4))),
                "vet@teste.local");

        verify(prescricaoRepository).save(any(Prescricao.class));
    }

    @Test
    void rn06_deveRecusarQuandoUmaDoseCoincideMesmoComInicioDiferente() {
        internacaoEm("ADMITIDA");
        Long antibiotico = novoId();
        Long analgesico = novoId();
        interacaoProibida(antibiotico, "Antibiótico A", analgesico, "Analgésico C");

        // A: 08h, 14h, 20h. C: 14h. Cruzam às 14h, na segunda dose do A.
        assertThatThrownBy(() -> prescricaoService.criar(
                pedido(item(antibiotico, 6, 3, AS_8H), item(analgesico, 24, 1, AS_14H)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("10/10 às 14:00");
    }

    @Test
    void rn06_deveRecusarMedicamentoNoMesmoHorarioDeOutroJaPrescritoNaInternacao() {
        internacaoEm("EM_TRATAMENTO");
        Long antibiotico = novoId();
        Long analgesico = novoId();
        interacaoProibida(analgesico, "Analgésico C", antibiotico, "Antibiótico A");
        when(prescricaoRepository.itensEmUso(INTERNACAO)).thenReturn(List.of(emUso(antibiotico, 8, 3, AS_14H)));

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(analgesico, 12, 1, AS_14H)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("RN-06")
                .hasMessageContaining("já prescrito nesta internação");
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void rn06_deveAceitarMedicamentoEmHorarioDiferenteDoJaPrescrito() {
        internacaoEm("EM_TRATAMENTO");
        Long antibiotico = novoId();
        Long analgesico = novoId();
        interacaoProibida(analgesico, "Analgésico C", antibiotico, "Antibiótico A");
        when(prescricaoRepository.itensEmUso(INTERNACAO)).thenReturn(List.of(emUso(antibiotico, 8, 3, AS_14H)));

        prescricaoService.criar(pedido(item(analgesico, 12, 1, AS_14H.plusHours(1))), "vet@teste.local");

        verify(prescricaoRepository).save(any(Prescricao.class));
    }

    @Test
    void rn06_deveConsultarCadaMedicamentoUmaVezSo() {
        internacaoEm("ADMITIDA");
        Long dipirona = novoId();

        prescricaoService.criar(pedido(item(dipirona, 8, 3, AS_8H), item(dipirona, 6, 2, AS_8H)), "vet@teste.local");

        verify(medicacaoClient).buscar(dipirona);
    }

    @Test
    void criar_naoDeveGravarQuandoOMedicamentoNaoExiste() {
        internacaoEm("ADMITIDA");
        Long inexistente = novoId();
        when(medicacaoClient.buscar(inexistente))
                .thenThrow(new RecursoNaoEncontradoException("Medicamento não encontrado: " + inexistente));

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(inexistente, 8, 3, AS_8H)), "vet@teste.local"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void buscar_deveLancarQuandoNaoExiste() {
        when(prescricaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescricaoService.buscar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void desativar_deveMarcarComoInativaSemApagarECancelarAsDosesPendentes() {
        Prescricao prescricao = Prescricao.builder().id(5L).internacaoId(INTERNACAO)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA).build();
        when(prescricaoRepository.findById(5L)).thenReturn(Optional.of(prescricao));

        PrescricaoResponse resposta = prescricaoService.desativar(5L);

        assertThat(resposta.ativo()).isFalse();
        verify(doseClient).cancelar(5L);
        verify(prescricaoRepository, never()).delete(any());
    }

    @Test
    void desativar_prescricaoJaInativaNaoChamaOMsdosesDeNovo() {
        Prescricao prescricao = Prescricao.builder().id(5L).internacaoId(INTERNACAO).ativo(false)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA).build();
        when(prescricaoRepository.findById(5L)).thenReturn(Optional.of(prescricao));

        prescricaoService.desativar(5L);

        verify(doseClient, never()).cancelar(anyLong());
    }

    private Prescricao desativada(Long medicacaoId) {
        return desativada(medicacaoId, AS_8H);
    }

    /** Prescrição desativada com um item de 3 doses a cada 8 h, a partir de `inicio` (o relógio dos testes é 09/10, 10h). */
    private Prescricao desativada(Long medicacaoId, LocalDateTime inicio) {
        Prescricao prescricao = Prescricao.builder().id(5L).internacaoId(INTERNACAO).ativo(false)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA).build();
        prescricao.adicionarItem(ItemPrescricao.builder().medicacaoId(medicacaoId).dosagem("5 mg").intervaloHoras(8)
                .quantidadeDoses(3).inicio(inicio).build());
        when(prescricaoRepository.findById(5L)).thenReturn(Optional.of(prescricao));
        return prescricao;
    }

    private static List<DoseDto> pendentes(int quantidade) {
        return java.util.stream.IntStream.rangeClosed(1, quantidade).mapToObj(i -> new DoseDto((long) i, "PENDENTE")).toList();
    }

    @Test
    void ativar_reativaAPrescricaoQuandoAsDosesVoltam() {
        internacaoEm("EM_TRATAMENTO");
        desativada(novoId());
        when(doseClient.reativar(5L)).thenReturn(pendentes(3));

        PrescricaoResponse resposta = prescricaoService.ativar(5L);

        assertThat(resposta.ativo()).isTrue();
        verify(doseClient).reativar(5L);
    }

    @Test
    void ativar_prescricaoJaAtivaNaoChamaNada() {
        Prescricao ativa = Prescricao.builder().id(5L).internacaoId(INTERNACAO).veterinarioEmail("vet@teste.local")
                .criadoEm(AGORA).build();
        when(prescricaoRepository.findById(5L)).thenReturn(Optional.of(ativa));

        assertThat(prescricaoService.ativar(5L).ativo()).isTrue();

        verify(doseClient, never()).reativar(anyLong());
        verify(internacaoClient, never()).buscar(anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALTA_AUTORIZADA", "ALTA_A_PEDIDO_DO_TUTOR", "ENCERRADA"})
    void ativar_recusaInternacaoComAltaOuEncerrada(String status) {
        internacaoEm(status);
        Prescricao prescricao = desativada(novoId());

        assertThatThrownBy(() -> prescricaoService.ativar(5L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining(status);
        assertThat(prescricao.isAtivo()).isFalse();
        verify(doseClient, never()).reativar(anyLong());
    }

    @Test
    void ativar_recusaQuandoJaNaoHaDoseParaAplicar() {
        internacaoEm("EM_TRATAMENTO");
        Prescricao prescricao = desativada(novoId());
        when(doseClient.reativar(5L)).thenReturn(List.of(new DoseDto(1L, "CANCELADA"), new DoseDto(2L, "PERDIDA")));

        assertThatThrownBy(() -> prescricaoService.ativar(5L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nova prescrição");
        assertThat(prescricao.isAtivo()).isFalse();
    }

    @Test
    void ativar_semSaldoOuComMsdosesForaNaoReativaNada() {
        internacaoEm("EM_TRATAMENTO");
        Prescricao prescricao = desativada(novoId());
        when(doseClient.reativar(5L)).thenThrow(new RegraDeNegocioException("Estoque insuficiente de X: 0 em estoque, 3 pedidas."));
        assertThatThrownBy(() -> prescricaoService.ativar(5L)).hasMessageContaining("Estoque insuficiente");
        assertThat(prescricao.isAtivo()).isFalse();

        doThrow(new ServicoIndisponivelException("octopus-msdoses não respondeu.")).when(doseClient).reativar(5L);
        assertThatThrownBy(() -> prescricaoService.ativar(5L)).isInstanceOf(ServicoIndisponivelException.class);
        assertThat(prescricao.isAtivo()).isFalse();
    }

    // RN-06 também vale ao reativar, mas só nos horários que ainda não passaram (relógio dos testes: 09/10, 10h).
    @Test
    void ativar_aplicaORn06ContraAsPrescricoesAtivasNosHorariosFuturos() {
        internacaoEm("EM_TRATAMENTO");
        Long antibiotico = novoId();
        Long analgesico = novoId();
        interacaoProibida(antibiotico, "Antibiótico A", analgesico, "Analgésico C");
        LocalDateTime as6h = LocalDateTime.of(2026, 10, 9, 6, 0); // horários 06h, 14h e 22h
        desativada(antibiotico, as6h);
        // Outra prescrição ativa, com o Analgésico C às 14h de hoje: horário futuro em comum.
        when(prescricaoRepository.itensEmUso(INTERNACAO))
                .thenReturn(List.of(emUso(analgesico, 24, 1, LocalDateTime.of(2026, 10, 9, 14, 0))));

        assertThatThrownBy(() -> prescricaoService.ativar(5L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("RN-06")
                .hasMessageContaining("já prescrito nesta internação");
        verify(doseClient, never()).reativar(anyLong());
    }

    @Test
    void ativar_ignoraNoRn06OHorarioQueJaPassou() {
        internacaoEm("EM_TRATAMENTO");
        Long antibiotico = novoId();
        Long analgesico = novoId();
        interacaoProibida(antibiotico, "Antibiótico A", analgesico, "Analgésico C");
        LocalDateTime as6h = LocalDateTime.of(2026, 10, 9, 6, 0);
        desativada(antibiotico, as6h);
        // O único horário em comum é 06h de hoje, quatro horas antes do relógio: já passou e não barra.
        when(prescricaoRepository.itensEmUso(INTERNACAO)).thenReturn(List.of(emUso(analgesico, 24, 1, as6h)));
        when(doseClient.reativar(5L)).thenReturn(pendentes(2));

        assertThat(prescricaoService.ativar(5L).ativo()).isTrue();
    }

    /** Prescrição ativa da internação, criada `minutosAtras` minutos antes do relógio dos testes. */
    private Prescricao ativaCriadaHa(long id, int minutosAtras) {
        Prescricao p = Prescricao.builder().id(id).internacaoId(INTERNACAO).veterinarioEmail("vet@teste.local")
                .criadoEm(AGORA.minusMinutes(minutosAtras)).build();
        when(prescricaoRepository.findByInternacaoIdOrderByCriadoEmDesc(INTERNACAO)).thenReturn(List.of(p));
        lenient().when(prescricaoRepository.findById(id)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void listar_concluiAPrescricaoQuandoOAnimalJaSaiu() {
        Prescricao p = ativaCriadaHa(5L, 600);
        internacaoEm("ENCERRADA");

        List<PrescricaoResponse> lista = prescricaoService.listar(INTERNACAO);

        assertThat(p.isAtivo()).isFalse();
        assertThat(p.getConcluidaEm()).isEqualTo(AGORA);
        assertThat(lista.get(0).situacao()).isEqualTo("CONCLUIDA");
        verify(doseClient, never()).situacao(any());
    }

    @Test
    void listar_concluiAPrescricaoSemDosePendenteNemSeNecessarioAtivo() {
        Prescricao p = ativaCriadaHa(5L, 600);
        internacaoEm("EM_TRATAMENTO");
        when(doseClient.situacao(List.of(5L))).thenReturn(List.of(new SituacaoDosesDto(5L, 0, false)));

        PrescricaoResponse resposta = prescricaoService.listar(INTERNACAO).get(0);

        assertThat(p.isAtivo()).isFalse();
        assertThat(resposta.ativo()).isFalse();
        assertThat(resposta.situacao()).isEqualTo("CONCLUIDA");
    }

    @Test
    void listar_naoConcluiEnquantoHaDosePendenteOuSeNecessarioAtivo() {
        Prescricao comPendente = ativaCriadaHa(5L, 600);
        internacaoEm("EM_TRATAMENTO");
        when(doseClient.situacao(List.of(5L))).thenReturn(List.of(new SituacaoDosesDto(5L, 2, false)));
        assertThat(prescricaoService.listar(INTERNACAO).get(0).situacao()).isEqualTo("ATIVA");
        assertThat(comPendente.isAtivo()).isTrue();

        when(doseClient.situacao(List.of(5L))).thenReturn(List.of(new SituacaoDosesDto(5L, 0, true)));
        assertThat(prescricaoService.listar(INTERNACAO).get(0).situacao()).isEqualTo("ATIVA");
    }

    @Test
    void listar_naoConcluiPrescricaoRecemCriadaPoisAsDosesPodemEstarSendoAgendadas() {
        Prescricao p = ativaCriadaHa(5L, 1);
        internacaoEm("EM_TRATAMENTO");

        assertThat(prescricaoService.listar(INTERNACAO).get(0).situacao()).isEqualTo("ATIVA");

        assertThat(p.isAtivo()).isTrue();
        verify(doseClient, never()).situacao(any());
    }

    @Test
    void listar_seOMsdosesOuAInternacaoNaoRespondemNadaMudaENaoFalha() {
        Prescricao p = ativaCriadaHa(5L, 600);
        when(internacaoClient.buscar(INTERNACAO)).thenThrow(new ServicoIndisponivelException("ms-internacao fora"));
        when(doseClient.situacao(any())).thenThrow(new ServicoIndisponivelException("msdoses fora"));

        assertThat(prescricaoService.listar(INTERNACAO)).hasSize(1);

        assertThat(p.isAtivo()).isTrue();
    }

    @Test
    void listar_naoMexeNaPrescricaoQueOVeterinarioDesativou() {
        Prescricao p = Prescricao.builder().id(5L).internacaoId(INTERNACAO).ativo(false)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA.minusMinutes(600)).build();
        when(prescricaoRepository.findByInternacaoIdOrderByCriadoEmDesc(INTERNACAO)).thenReturn(List.of(p));

        assertThat(prescricaoService.listar(INTERNACAO).get(0).situacao()).isEqualTo("DESATIVADA");

        verify(internacaoClient, never()).buscar(anyLong());
        verify(doseClient, never()).situacao(any());
    }

    @Test
    void ativar_recusaPrescricaoJaConcluidaPoisNaoHaMaisDoses() {
        Prescricao p = Prescricao.builder().id(5L).internacaoId(INTERNACAO).ativo(false).concluidaEm(AGORA)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA.minusMinutes(600)).build();
        when(prescricaoRepository.findById(5L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> prescricaoService.ativar(5L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("concluída");
        verify(doseClient, never()).reativar(anyLong());
    }

    private ItemPrescricaoRequest seNecessario(Long medicacaoId, int intervaloMinimo) {
        return new ItemPrescricaoRequest(medicacaoId, "1 comprimido", true, intervaloMinimo, null, null, null);
    }

    // Mudança contratada: o item "se necessário" só passa a existir em 01/11/2026 (o relógio dos testes está em 09/10).
    @Test
    void criar_seNecessarioAntesDe01_11ERecusado() {
        internacaoEm("ADMITIDA");

        assertThatThrownBy(() -> prescricaoService.criar(pedido(seNecessario(novoId(), 6)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("01/11/2026");
        verify(internacaoClient, never()).iniciarTratamento(anyLong());
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void criar_seNecessarioEmVigorVaiAoMsdosesSemHorariosEComOIntervaloMinimo() {
        Clock clock = Clock.fixed(AGORA.atZone(SAO_PAULO).toInstant(), SAO_PAULO);
        prescricaoService = new PrescricaoService(prescricaoRepository, internacaoClient, medicacaoClient, doseClient,
                transacao, clock, java.time.LocalDate.of(2026, 10, 1));
        internacaoEm("EM_TRATAMENTO");
        Long dipirona = novoId();
        when(medicacaoClient.buscar(dipirona))
                .thenReturn(new MedicacaoDto(dipirona, "Dipirona", true, 1, "SINTOMATICO", List.of()));

        PrescricaoResponse resposta = prescricaoService.criar(pedido(seNecessario(dipirona, 6),
                item(novoId(), 8, 2, AS_8H)), "vet@teste.local");

        ArgumentCaptor<AgendamentoDosesDto> agendamento = ArgumentCaptor.forClass(AgendamentoDosesDto.class);
        verify(doseClient).agendar(agendamento.capture());
        AgendamentoDosesDto.Item item = agendamento.getValue().itens().get(0);
        assertThat(item.seNecessario()).isTrue();
        assertThat(item.esquema()).isEqualTo("SINTOMATICO");
        assertThat(item.intervaloMinimoHoras()).isEqualTo(6);
        assertThat(item.horarios()).isEmpty();
        assertThat(agendamento.getValue().itens().get(1).esquema()).isEqualTo("CONTINUO");
        assertThat(resposta.itens().get(0).seNecessario()).isTrue();
        assertThat(resposta.itens().get(0).quantidadeDoses()).isNull();
    }

    @Test
    void criar_seNecessarioSemEstoqueNenhumERecusado() {
        prescricaoService = new PrescricaoService(prescricaoRepository, internacaoClient, medicacaoClient, doseClient,
                transacao, Clock.fixed(AGORA.atZone(SAO_PAULO).toInstant(), SAO_PAULO),
                java.time.LocalDate.of(2026, 10, 1));
        internacaoEm("ADMITIDA");
        Long dipirona = novoId();
        when(medicacaoClient.buscar(dipirona))
                .thenReturn(new MedicacaoDto(dipirona, "Dipirona", true, 0, "SINTOMATICO", List.of()));

        assertThatThrownBy(() -> prescricaoService.criar(pedido(seNecessario(dipirona, 6)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Estoque insuficiente de Dipirona");
    }
}
