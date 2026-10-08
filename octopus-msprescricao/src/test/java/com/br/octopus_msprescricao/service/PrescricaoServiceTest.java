package com.br.octopus_msprescricao.service;

import com.br.octopus_msprescricao.domain.Dose;
import com.br.octopus_msprescricao.domain.ItemPrescricao;
import com.br.octopus_msprescricao.domain.Prescricao;
import com.br.octopus_msprescricao.domain.enums.StatusDose;
import com.br.octopus_msprescricao.dto.request.ItemPrescricaoRequest;
import com.br.octopus_msprescricao.dto.request.PrescricaoRequest;
import com.br.octopus_msprescricao.dto.response.PrescricaoResponse;
import com.br.octopus_msprescricao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msprescricao.repository.PrescricaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescricaoServiceTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 10, 8, 0);

    @Mock
    private PrescricaoRepository prescricaoRepository;

    @InjectMocks
    private PrescricaoService prescricaoService;

    private ItemPrescricaoRequest item(int intervaloHoras, int quantidadeDoses) {
        return new ItemPrescricaoRequest(UUID.randomUUID(), "5 mg", intervaloHoras, quantidadeDoses, INICIO, null);
    }

    @Test
    void criar_deveGerarUmaDosePorHorarioRespeitandoOIntervalo() {
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));
        var request = new PrescricaoRequest(UUID.randomUUID(), null, List.of(item(8, 3)));

        PrescricaoResponse response = prescricaoService.criar(request, "vet@vidapet.com");

        ArgumentCaptor<Prescricao> captor = ArgumentCaptor.forClass(Prescricao.class);
        verify(prescricaoRepository).save(captor.capture());
        List<Dose> doses = captor.getValue().getItens().get(0).getDoses();
        assertThat(doses).extracting(Dose::getHorarioPrevisto).containsExactly(
                LocalDateTime.of(2026, 10, 10, 8, 0),
                LocalDateTime.of(2026, 10, 10, 16, 0),
                LocalDateTime.of(2026, 10, 11, 0, 0));
        assertThat(doses).extracting(Dose::getStatus).containsOnly(StatusDose.PENDENTE);
        assertThat(response.itens().get(0).doses()).hasSize(3);
    }

    @Test
    void criar_deveGerarAsDosesDeCadaItemSeparadamente() {
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));
        var request = new PrescricaoRequest(UUID.randomUUID(), null, List.of(item(12, 2), item(6, 4)));

        prescricaoService.criar(request, "vet@vidapet.com");

        ArgumentCaptor<Prescricao> captor = ArgumentCaptor.forClass(Prescricao.class);
        verify(prescricaoRepository).save(captor.capture());
        List<ItemPrescricao> itens = captor.getValue().getItens();
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).getDoses()).hasSize(2);
        assertThat(itens.get(1).getDoses()).hasSize(4);
        assertThat(itens.get(1).getDoses().get(3).getHorarioPrevisto()).isEqualTo(INICIO.plusHours(18));
    }

    @Test
    void criar_deveGravarOEmailDoVeterinarioDoTokenELigarOsItensAPrescricao() {
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID internacaoId = UUID.randomUUID();
        var request = new PrescricaoRequest(internacaoId, "Animal agitado", List.of(item(8, 1)));

        PrescricaoResponse response = prescricaoService.criar(request, "vet@vidapet.com");

        ArgumentCaptor<Prescricao> captor = ArgumentCaptor.forClass(Prescricao.class);
        verify(prescricaoRepository).save(captor.capture());
        Prescricao salva = captor.getValue();
        assertThat(salva.getVeterinarioEmail()).isEqualTo("vet@vidapet.com");
        assertThat(salva.getInternacaoId()).isEqualTo(internacaoId);
        assertThat(salva.isAtivo()).isTrue();
        assertThat(salva.getItens().get(0).getPrescricao()).isSameAs(salva);
        assertThat(response.veterinarioEmail()).isEqualTo("vet@vidapet.com");
    }

    @Test
    void buscar_deveLancarQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(prescricaoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescricaoService.buscar(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void desativar_deveMarcarComoInativaSemApagar() {
        UUID id = UUID.randomUUID();
        Prescricao prescricao = Prescricao.builder().id(id).internacaoId(UUID.randomUUID())
                .veterinarioEmail("vet@vidapet.com").build();
        when(prescricaoRepository.findById(id)).thenReturn(Optional.of(prescricao));

        PrescricaoResponse response = prescricaoService.desativar(id);

        assertThat(response.ativo()).isFalse();
        assertThat(prescricao.isAtivo()).isFalse();
    }
}
