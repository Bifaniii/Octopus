package com.br.octopus_msprescricao.service;

import com.br.octopus_msprescricao.client.DoseClient;
import com.br.octopus_msprescricao.client.InternacaoClient;
import com.br.octopus_msprescricao.client.MedicacaoClient;
import com.br.octopus_msprescricao.client.dto.AgendamentoDosesDto;
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
                transacao, clock);
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
                .thenAnswer(inv -> new MedicacaoDto(inv.getArgument(0), "Medicamento", true, List.of()));
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
        return new ItemPrescricaoRequest(medicacaoId, "5 mg", intervaloHoras, quantidadeDoses, inicio, null);
    }

    /** Item de uma prescrição ativa já gravada na internação. */
    private ItemPrescricao emUso(Long medicacaoId, int intervaloHoras, int quantidadeDoses, LocalDateTime inicio) {
        return ItemPrescricao.builder().medicacaoId(medicacaoId).dosagem("5 mg").intervaloHoras(intervaloHoras)
                .quantidadeDoses(quantidadeDoses).inicio(inicio).build();
    }

    /** Cadastra no mock do msmedications um par com interação proibida, gravado nos dois sentidos. */
    private void interacaoProibida(Long a, String nomeA, Long b, String nomeB) {
        lenient().when(medicacaoClient.buscar(a)).thenReturn(
                new MedicacaoDto(a, nomeA, true, List.of(new MedicacaoDto.Interacao(b, nomeB))));
        lenient().when(medicacaoClient.buscar(b)).thenReturn(
                new MedicacaoDto(b, nomeB, true, List.of(new MedicacaoDto.Interacao(a, nomeA))));
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
    void criar_deveIniciarOTratamentoNaInternacaoAntesDeGravar() {
        internacaoEm("ADMITIDA");

        prescricaoService.criar(pedido(item(8, 3)), "vet@teste.local");

        InOrder ordem = inOrder(internacaoClient, prescricaoRepository);
        ordem.verify(internacaoClient).buscar(INTERNACAO);
        ordem.verify(internacaoClient).iniciarTratamento(INTERNACAO);
        ordem.verify(prescricaoRepository).save(any(Prescricao.class));
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
}
