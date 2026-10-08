package com.br.octopus_msinternacao.service;

import com.br.octopus_msinternacao.client.AnimalClient;
import com.br.octopus_msinternacao.client.BaiaClient;
import com.br.octopus_msinternacao.client.dto.AnimalDto;
import com.br.octopus_msinternacao.client.dto.BaiaDto;
import com.br.octopus_msinternacao.client.dto.TipoBaia;
import com.br.octopus_msinternacao.domain.Internacao;
import com.br.octopus_msinternacao.dto.request.AdmissaoRequest;
import com.br.octopus_msinternacao.dto.request.IsolamentoRequest;
import com.br.octopus_msinternacao.dto.response.InternacaoResponse;
import com.br.octopus_msinternacao.exception.RecursoDuplicadoException;
import com.br.octopus_msinternacao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msinternacao.exception.RegraNegocioException;
import com.br.octopus_msinternacao.repository.InternacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.br.octopus_msinternacao.domain.enums.StatusInternacao.ADMITIDA;
import static com.br.octopus_msinternacao.domain.enums.StatusInternacao.ENCERRADA;
import static com.br.octopus_msinternacao.domain.enums.StatusInternacao.ISOLAMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// RN-01 e RN-02 com os outros módulos mockados e o relógio fixo em 08/10/2026 10:00 (São Paulo).
@ExtendWith(MockitoExtension.class)
class InternacaoServiceTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 8, 10, 0);
    private static final LocalDate HOJE = AGORA.toLocalDate();
    private static final String RECEPCAO = "recepcao@vidapet.local";
    private static final String VET = "vet@vidapet.local";

    @Mock
    private InternacaoRepository repository;
    @Mock
    private AnimalClient animalClient;
    @Mock
    private BaiaClient baiaClient;
    @Mock
    private TransactionTemplate transacao;

    private InternacaoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AGORA.atZone(SAO_PAULO).toInstant(), SAO_PAULO);
        service = new InternacaoService(repository, animalClient, baiaClient, transacao, clock);
        // O TransactionTemplate mockado só executa o bloco, como faria o de verdade sem banco.
        lenient().when(transacao.execute(any())).thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        lenient().when(repository.save(any(Internacao.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AnimalDto animal(LocalDate vacina, UUID maeId) {
        AnimalDto animal = new AnimalDto(UUID.randomUUID(), "Thor", "Cão", vacina, maeId);
        lenient().when(animalClient.buscar(animal.id())).thenReturn(animal);
        return animal;
    }

    private BaiaDto baia(TipoBaia tipo, int capacidade) {
        BaiaDto baia = new BaiaDto(UUID.randomUUID(), tipo, "B-01", capacidade, true);
        lenient().when(baiaClient.buscar(baia.id())).thenReturn(baia);
        return baia;
    }

    private void ocupantes(BaiaDto baia, Internacao... internacoes) {
        when(repository.findByBaiaIdAndStatusNot(baia.id(), ENCERRADA)).thenReturn(List.of(internacoes));
    }

    private Internacao ocupante(BaiaDto baia, UUID maeId) {
        return Internacao.admitir(UUID.randomUUID(), "Nina", "Cão", maeId, baia.id(), "Internado antes", RECEPCAO, AGORA.minusDays(1));
    }

    private InternacaoResponse admitir(AnimalDto animal, BaiaDto baia) {
        return service.admitir(new AdmissaoRequest(animal.id(), baia.id(), "Gastroenterite"), RECEPCAO);
    }

    // ---------- admissão ----------

    @Test
    @DisplayName("admissão válida: grava ADMITIDA com o usuário do token e a hora da clínica")
    void admissaoValida() {
        AnimalDto animal = animal(HOJE.minusMonths(3), null);
        BaiaDto baia = baia(TipoBaia.COLETIVA, 1);
        ocupantes(baia);

        InternacaoResponse resposta = admitir(animal, baia);

        assertThat(resposta.status()).isEqualTo(ADMITIDA);
        assertThat(resposta.animalId()).isEqualTo(animal.id());
        assertThat(resposta.animalNome()).isEqualTo("Thor");
        assertThat(resposta.animalEspecie()).isEqualTo("Cão");
        assertThat(resposta.baiaId()).isEqualTo(baia.id());
        assertThat(resposta.registradoPor()).isEqualTo(RECEPCAO);
        assertThat(resposta.dataAdmissao()).isEqualTo(AGORA);
        verify(repository).save(any(Internacao.class));
    }

    @Test
    @DisplayName("animal com internação aberta: 409 e nada é gravado")
    void animalJaInternado() {
        AnimalDto animal = animal(HOJE.minusMonths(3), null);
        BaiaDto baia = baia(TipoBaia.COLETIVA, 1);
        when(repository.existsByAnimalIdAndStatusNot(animal.id(), ENCERRADA)).thenReturn(true);

        assertThatThrownBy(() -> admitir(animal, baia)).isInstanceOf(RecursoDuplicadoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("baia desativada: recusada antes de abrir a transação")
    void baiaDesativada() {
        AnimalDto animal = animal(HOJE.minusMonths(3), null);
        BaiaDto baia = new BaiaDto(UUID.randomUUID(), TipoBaia.COLETIVA, "B-02", 1, false);
        when(baiaClient.buscar(baia.id())).thenReturn(baia);

        assertThatThrownBy(() -> admitir(animal, baia))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("desativada");
        verifyNoInteractions(transacao);
    }

    // ---------- RN-02 ----------

    @Test
    @DisplayName("RN-02: sem registro de antirrábica não entra em coletiva")
    void rn02SemVacina() {
        AnimalDto animal = animal(null, null);
        BaiaDto baia = baia(TipoBaia.COLETIVA, 1);

        assertThatThrownBy(() -> admitir(animal, baia))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("RN-02");
        verifyNoInteractions(transacao, repository);
    }

    @Test
    @DisplayName("RN-02: antirrábica com 12 meses e 1 dia não entra em coletiva")
    void rn02VacinaVencida() {
        AnimalDto animal = animal(HOJE.minusMonths(12).minusDays(1), null);
        BaiaDto baia = baia(TipoBaia.COLETIVA, 1);

        assertThatThrownBy(() -> admitir(animal, baia)).hasMessageContaining("RN-02");
    }

    @Test
    @DisplayName("RN-02: antirrábica com exatamente 12 meses ainda vale")
    void rn02VacinaNoLimite() {
        AnimalDto animal = animal(HOJE.minusMonths(12), null);
        BaiaDto baia = baia(TipoBaia.COLETIVA, 1);
        ocupantes(baia);

        assertThat(admitir(animal, baia).status()).isEqualTo(ADMITIDA);
    }

    @Test
    @DisplayName("RN-02: antirrábica vencida entra em baia de isolamento")
    void rn02VencidaNoIsolamento() {
        AnimalDto animal = animal(null, null);
        BaiaDto baia = baia(TipoBaia.ISOLAMENTO, 1);
        ocupantes(baia);

        assertThat(admitir(animal, baia).status()).isEqualTo(ADMITIDA);
    }

    // ---------- RN-01 ----------

    @Test
    @DisplayName("RN-01: coletiva com um animal está lotada")
    void rn01ColetivaLotada() {
        AnimalDto animal = animal(HOJE.minusMonths(3), null);
        BaiaDto baia = baia(TipoBaia.COLETIVA, 1);
        ocupantes(baia, ocupante(baia, null));

        assertThatThrownBy(() -> admitir(animal, baia))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("RN-01")
                .hasMessageContaining("1/1");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("RN-01: coletiva cadastrada com capacidade 6 (dado antigo) aceita só 1")
    void rn01CapacidadeEfetiva() {
        AnimalDto animal = animal(HOJE.minusMonths(3), null);
        BaiaDto baia = baia(TipoBaia.COLETIVA, 6);
        ocupantes(baia, ocupante(baia, null));

        assertThatThrownBy(() -> admitir(animal, baia)).hasMessageContaining("1/1");
    }

    @Test
    @DisplayName("RN-01: ninhada aceita mais um filhote da mesma mãe")
    void rn01NinhadaMesmaMae() {
        UUID mae = UUID.randomUUID();
        AnimalDto filhote = animal(HOJE.minusMonths(1), mae);
        BaiaDto baia = baia(TipoBaia.NINHADA, 6);
        ocupantes(baia, ocupante(baia, mae), ocupante(baia, mae));

        InternacaoResponse resposta = admitir(filhote, baia);

        assertThat(resposta.maeId()).isEqualTo(mae);
    }

    @Test
    @DisplayName("RN-01: ninhada recusa filhote de outra mãe")
    void rn01NinhadaOutraMae() {
        AnimalDto filhote = animal(HOJE.minusMonths(1), UUID.randomUUID());
        BaiaDto baia = baia(TipoBaia.NINHADA, 6);
        ocupantes(baia, ocupante(baia, UUID.randomUUID()));

        assertThatThrownBy(() -> admitir(filhote, baia)).hasMessageContaining("outra mãe");
    }

    @Test
    @DisplayName("RN-01: ninhada recusa filhote sem mãe cadastrada")
    void rn01NinhadaSemMae() {
        AnimalDto filhote = animal(HOJE.minusMonths(1), null);
        BaiaDto baia = baia(TipoBaia.NINHADA, 6);
        ocupantes(baia);

        assertThatThrownBy(() -> admitir(filhote, baia)).hasMessageContaining("mãe cadastrada");
    }

    @Test
    @DisplayName("RN-01: ninhada com 6 filhotes está lotada")
    void rn01NinhadaLotada() {
        UUID mae = UUID.randomUUID();
        AnimalDto filhote = animal(HOJE.minusMonths(1), mae);
        BaiaDto baia = baia(TipoBaia.NINHADA, 6);
        ocupantes(baia, ocupante(baia, mae), ocupante(baia, mae), ocupante(baia, mae),
                ocupante(baia, mae), ocupante(baia, mae), ocupante(baia, mae));

        assertThatThrownBy(() -> admitir(filhote, baia)).hasMessageContaining("6/6");
    }

    // ---------- isolamento ----------

    private Internacao internacaoEmTratamento(AnimalDto animal, UUID baiaId) {
        Internacao internacao = Internacao.admitir(animal.id(), animal.nome(), animal.especie(), null, baiaId, "Cinomose", RECEPCAO, AGORA.minusDays(1));
        internacao.iniciarTratamento(VET, AGORA.minusHours(20));
        // Fora do banco o id não é gerado; o service só precisa achar a internação por ele.
        ReflectionTestUtils.setField(internacao, "id", UUID.randomUUID());
        when(repository.findById(internacao.getId())).thenReturn(Optional.of(internacao));
        return internacao;
    }

    @Test
    @DisplayName("isolar: vacina irregular vai para baia de isolamento livre")
    void isolarValido() {
        AnimalDto animal = animal(null, null);
        Internacao internacao = internacaoEmTratamento(animal, UUID.randomUUID());
        BaiaDto isolamento = baia(TipoBaia.ISOLAMENTO, 1);
        ocupantes(isolamento);

        InternacaoResponse resposta = service.isolar(internacao.getId(), new IsolamentoRequest(isolamento.id()), VET);

        assertThat(resposta.status()).isEqualTo(ISOLAMENTO);
        assertThat(resposta.baiaId()).isEqualTo(isolamento.id());
    }

    @Test
    @DisplayName("isolar: animal com vacina em dia não vai para o isolamento (RN-02)")
    void isolarComVacinaEmDia() {
        AnimalDto animal = animal(HOJE.minusMonths(2), null);
        Internacao internacao = internacaoEmTratamento(animal, UUID.randomUUID());
        BaiaDto isolamento = baia(TipoBaia.ISOLAMENTO, 1);

        assertThatThrownBy(() -> service.isolar(internacao.getId(), new IsolamentoRequest(isolamento.id()), VET))
                .hasMessageContaining("RN-02");
        verifyNoInteractions(transacao);
    }

    @Test
    @DisplayName("isolar: a baia de destino precisa ser de isolamento")
    void isolarEmBaiaColetiva() {
        AnimalDto animal = animal(null, null);
        Internacao internacao = internacaoEmTratamento(animal, UUID.randomUUID());
        BaiaDto coletiva = baia(TipoBaia.COLETIVA, 1);

        assertThatThrownBy(() -> service.isolar(internacao.getId(), new IsolamentoRequest(coletiva.id()), VET))
                .hasMessageContaining("não é de isolamento");
    }

    @Test
    @DisplayName("isolar: baia de isolamento ocupada por outro animal está lotada (RN-01)")
    void isolarEmBaiaOcupada() {
        AnimalDto animal = animal(null, null);
        Internacao internacao = internacaoEmTratamento(animal, UUID.randomUUID());
        BaiaDto isolamento = baia(TipoBaia.ISOLAMENTO, 1);
        ocupantes(isolamento, ocupante(isolamento, null));

        assertThatThrownBy(() -> service.isolar(internacao.getId(), new IsolamentoRequest(isolamento.id()), VET))
                .hasMessageContaining("RN-01");
    }

    @Test
    @DisplayName("isolar na própria baia (admitido já no isolamento): só muda o status, sem conferir vaga")
    void isolarNaMesmaBaia() {
        AnimalDto animal = animal(null, null);
        BaiaDto isolamento = baia(TipoBaia.ISOLAMENTO, 1);
        Internacao internacao = internacaoEmTratamento(animal, isolamento.id());

        InternacaoResponse resposta = service.isolar(internacao.getId(), new IsolamentoRequest(isolamento.id()), VET);

        assertThat(resposta.status()).isEqualTo(ISOLAMENTO);
        verify(repository, never()).findByBaiaIdAndStatusNot(any(), any());
    }

    @Test
    @DisplayName("internação inexistente: 404")
    void internacaoInexistente() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }
}
