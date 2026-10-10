package com.br.octopus_msprescricao.service;

import com.br.octopus_msprescricao.client.InternacaoClient;
import com.br.octopus_msprescricao.client.MedicacaoClient;
import com.br.octopus_msprescricao.client.dto.InternacaoDto;
import com.br.octopus_msprescricao.client.dto.MedicacaoDto;
import com.br.octopus_msprescricao.domain.Prescricao;
import com.br.octopus_msprescricao.dto.request.ItemPrescricaoRequest;
import com.br.octopus_msprescricao.dto.request.PrescricaoRequest;
import com.br.octopus_msprescricao.dto.response.PrescricaoResponse;
import com.br.octopus_msprescricao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msprescricao.exception.RegraDeNegocioException;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Prescrição com o ms-internacao mockado e o relógio fixo em 09/10/2026 10:00 (São Paulo).
@ExtendWith(MockitoExtension.class)
class PrescricaoServiceTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 9, 10, 0);
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final Long INTERNACAO = 7L;
    // Ids de medicamento sequenciais: nunca repetem e não colidem com INTERNACAO.
    private static final AtomicLong PROXIMO_ID = new AtomicLong(100);

    @Mock
    private PrescricaoRepository prescricaoRepository;
    @Mock
    private InternacaoClient internacaoClient;
    @Mock
    private MedicacaoClient medicacaoClient;

    private PrescricaoService prescricaoService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AGORA.atZone(SAO_PAULO).toInstant(), SAO_PAULO);
        prescricaoService = new PrescricaoService(prescricaoRepository, internacaoClient, medicacaoClient, clock);
        lenient().when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));
        // Por padrão, todo medicamento existe e não tem interação proibida.
        lenient().when(medicacaoClient.buscar(anyLong()))
                .thenAnswer(inv -> new MedicacaoDto(inv.getArgument(0), "Medicamento", true, List.of()));
    }

    private static Long novoId() {
        return PROXIMO_ID.incrementAndGet();
    }

    private void internacaoEm(String status) {
        when(internacaoClient.buscar(INTERNACAO)).thenReturn(new InternacaoDto(INTERNACAO, status, "Thor"));
    }

    private ItemPrescricaoRequest item(int intervaloHoras) {
        return item(novoId(), intervaloHoras);
    }

    private ItemPrescricaoRequest item(Long medicacaoId, int intervaloHoras) {
        return new ItemPrescricaoRequest(medicacaoId, intervaloHoras, INICIO, null);
    }

    /**
     * Cadastra no mock do msmedications um par com interação proibida, gravado nos dois sentidos.
     */
    private void interacaoProibida(Long a, String nomeA, Long b, String nomeB) {
        when(medicacaoClient.buscar(a)).thenReturn(
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

        PrescricaoResponse resposta = prescricaoService.criar(pedido(item(8), item(12)), "vet@teste.local");

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
    void criar_deveIniciarOTratamentoNaInternacaoAntesDeGravar() {
        internacaoEm("ADMITIDA");

        prescricaoService.criar(pedido(item(8)), "vet@teste.local");

        InOrder ordem = inOrder(internacaoClient, prescricaoRepository);
        ordem.verify(internacaoClient).buscar(INTERNACAO);
        ordem.verify(internacaoClient).iniciarTratamento(INTERNACAO);
        ordem.verify(prescricaoRepository).save(any(Prescricao.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EM_TRATAMENTO", "ISOLAMENTO"})
    void criar_deveAceitarInternacaoJaEmTratamento(String status) {
        internacaoEm(status);

        prescricaoService.criar(pedido(item(8)), "vet@teste.local");

        verify(prescricaoRepository).save(any(Prescricao.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALTA_AUTORIZADA", "ALTA_A_PEDIDO_DO_TUTOR", "ENCERRADA"})
    void criar_deveRecusarInternacaoComAltaOuEncerrada(String status) {
        internacaoEm(status);

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(8)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining(status);
        verify(internacaoClient, never()).iniciarTratamento(anyLong());
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void criar_naoDeveGravarQuandoAInternacaoNaoExiste() {
        when(internacaoClient.buscar(INTERNACAO))
                .thenThrow(new RecursoNaoEncontradoException("Internação não encontrada: " + INTERNACAO));

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(8)), "vet@teste.local"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void rn03_deveRecusarDoisMedicamentosComInteracaoNaMesmaPrescricao() {
        internacaoEm("ADMITIDA");
        Long meloxicam = novoId();
        Long cetoprofeno = novoId();
        interacaoProibida(meloxicam, "Meloxicam", cetoprofeno, "Cetoprofeno");

        assertThatThrownBy(() -> prescricaoService.criar(
                pedido(item(meloxicam, 24), item(cetoprofeno, 12)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("RN-03")
                .hasMessageContaining("Meloxicam")
                .hasMessageContaining("Cetoprofeno");
        verify(internacaoClient, never()).iniciarTratamento(anyLong());
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void rn03_deveRecusarMedicamentoComInteracaoComOutroJaPrescritoNaInternacao() {
        internacaoEm("EM_TRATAMENTO");
        Long meloxicam = novoId();
        Long cetoprofeno = novoId();
        interacaoProibida(cetoprofeno, "Cetoprofeno", meloxicam, "Meloxicam");
        when(prescricaoRepository.medicacoesEmUso(INTERNACAO)).thenReturn(List.of(meloxicam));

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(cetoprofeno, 12)), "vet@teste.local"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("RN-03")
                .hasMessageContaining("já prescrito nesta internação");
        verify(prescricaoRepository, never()).save(any());
    }

    @Test
    void rn03_deveAceitarMedicamentosSemInteracaoEntreSiNemComOsEmUso() {
        internacaoEm("EM_TRATAMENTO");
        when(prescricaoRepository.medicacoesEmUso(INTERNACAO)).thenReturn(List.of(novoId()));

        prescricaoService.criar(pedido(item(8), item(12)), "vet@teste.local");

        verify(prescricaoRepository).save(any(Prescricao.class));
    }

    @Test
    void rn03_deveConsultarCadaMedicamentoUmaVezSo() {
        internacaoEm("ADMITIDA");
        Long dipirona = novoId();

        prescricaoService.criar(pedido(item(dipirona, 8), item(dipirona, 6)), "vet@teste.local");

        verify(medicacaoClient).buscar(dipirona);
    }

    @Test
    void criar_naoDeveGravarQuandoOMedicamentoNaoExiste() {
        internacaoEm("ADMITIDA");
        Long inexistente = novoId();
        when(medicacaoClient.buscar(inexistente))
                .thenThrow(new RecursoNaoEncontradoException("Medicamento não encontrado: " + inexistente));

        assertThatThrownBy(() -> prescricaoService.criar(pedido(item(inexistente, 8)), "vet@teste.local"))
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
    void desativar_deveMarcarComoInativaSemApagar() {
        Prescricao prescricao = Prescricao.builder().id(5L).internacaoId(INTERNACAO)
                .veterinarioEmail("vet@teste.local").criadoEm(AGORA).build();
        when(prescricaoRepository.findById(5L)).thenReturn(Optional.of(prescricao));

        PrescricaoResponse resposta = prescricaoService.desativar(5L);

        assertThat(resposta.ativo()).isFalse();
        verify(prescricaoRepository, never()).delete(any());
    }
}