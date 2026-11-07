package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.Tutor;
import com.br.octopus_msusuario.domain.enums.Especie;
import com.br.octopus_msusuario.domain.enums.SexoAnimal;
import com.br.octopus_msusuario.dto.request.AnimalRequest;
import com.br.octopus_msusuario.dto.response.AnimalResponse;
import com.br.octopus_msusuario.exception.AnimalNaoEncontradoException;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.exception.RegraNegocioException;
import com.br.octopus_msusuario.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private TutorService tutorService;

    @InjectMocks
    private AnimalService animalService;

    @Test
    void criar_deveSalvarAnimalComEspecieEVacinaVinculadoAoTutor() {
        Long tutorId = 1L;
        Tutor tutor = Tutor.builder().id(tutorId).nome("João").build();
        when(tutorService.obter(tutorId)).thenReturn(tutor);
        when(animalRepository.save(any(Animal.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate vacina = LocalDate.of(2026, 3, 1);

        AnimalResponse response = animalService.criar(tutorId, new AnimalRequest("Rex", Especie.CAO, SexoAnimal.FEMEA, vacina, null));

        ArgumentCaptor<Animal> captor = ArgumentCaptor.forClass(Animal.class);
        verify(animalRepository).save(captor.capture());
        assertThat(captor.getValue().getEspecie()).isEqualTo(Especie.CAO);
        assertThat(captor.getValue().getDataUltimaAntirrabica()).isEqualTo(vacina);
        assertThat(response.tutorId()).isEqualTo(tutorId);
        assertThat(tutor.getAnimais()).containsExactly(captor.getValue());
    }

    @Test
    void criar_naoDeveSalvarQuandoTutorNaoExiste() {
        Long tutorId = 1L;
        when(tutorService.obter(tutorId)).thenThrow(new RecursoNaoEncontradoException("Tutor não encontrado: " + tutorId));

        assertThatThrownBy(() -> animalService.criar(tutorId, new AnimalRequest("Rex", Especie.CAO, SexoAnimal.FEMEA, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(animalRepository, never()).save(any());
    }

    @Test
    void criar_deveVincularAMaeInformada() {
        Long tutorId = 1L;
        Tutor tutor = Tutor.builder().id(tutorId).nome("João").build();
        Animal mae = Animal.builder().id(10L).nome("Nina").tutor(tutor).especie(Especie.CAO).sexo(SexoAnimal.FEMEA).build();
        when(tutorService.obter(tutorId)).thenReturn(tutor);
        when(animalRepository.buscarMae(mae.getId())).thenCallRealMethod();
        when(animalRepository.findById(mae.getId())).thenReturn(Optional.of(mae));
        when(animalRepository.save(any(Animal.class))).thenAnswer(inv -> inv.getArgument(0));

        AnimalResponse response = animalService.criar(tutorId, new AnimalRequest("Bidu", Especie.CAO, SexoAnimal.FEMEA, null, mae.getId()));

        assertThat(response.maeId()).isEqualTo(mae.getId());
    }

    @Test
    void criar_naoDeveSalvarQuandoMaeNaoExiste() {
        Long tutorId = 1L;
        Long maeId = 10L;
        when(tutorService.obter(tutorId)).thenReturn(Tutor.builder().id(tutorId).nome("João").build());
        when(animalRepository.buscarMae(maeId)).thenCallRealMethod();
        when(animalRepository.findById(maeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.criar(tutorId, new AnimalRequest("Bidu", Especie.CAO, SexoAnimal.FEMEA, null, maeId)))
                .isInstanceOf(AnimalNaoEncontradoException.class)
                .hasMessageContaining(maeId.toString());
        verify(animalRepository, never()).save(any());
    }

    private Animal maeCadastrada(Long id, String nome, Especie especie, SexoAnimal sexo) {
        Animal mae = Animal.builder().id(id).nome(nome).tutor(Tutor.builder().id(1L).nome("João").build())
                .especie(especie).sexo(sexo).build();
        when(animalRepository.buscarMae(id)).thenCallRealMethod();
        when(animalRepository.findById(id)).thenReturn(Optional.of(mae));
        return mae;
    }

    @Test
    void criar_recusaMaeMacho() {
        when(tutorService.obter(1L)).thenReturn(Tutor.builder().id(1L).nome("João").build());
        maeCadastrada(10L, "Thor", Especie.CAO, SexoAnimal.MACHO);

        assertThatThrownBy(() -> animalService.criar(1L,
                new AnimalRequest("Bidu", Especie.CAO, SexoAnimal.MACHO, null, 10L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Thor é macho");
        verify(animalRepository, never()).save(any());
    }

    @Test
    void criar_recusaMaeDeOutraEspecie() {
        when(tutorService.obter(1L)).thenReturn(Tutor.builder().id(1L).nome("João").build());
        maeCadastrada(10L, "Mia", Especie.GATO, SexoAnimal.FEMEA);

        assertThatThrownBy(() -> animalService.criar(1L,
                new AnimalRequest("Bidu", Especie.CAO, SexoAnimal.MACHO, null, 10L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("mesma espécie")
                .hasMessageContaining("Gato");
        verify(animalRepository, never()).save(any());
    }

    @Test
    void criar_recusaMaeCadastradaAntesDoSexoSerObrigatorio() {
        when(tutorService.obter(1L)).thenReturn(Tutor.builder().id(1L).nome("João").build());
        maeCadastrada(10L, "Nina", Especie.CAO, null);

        assertThatThrownBy(() -> animalService.criar(1L,
                new AnimalRequest("Bidu", Especie.CAO, SexoAnimal.MACHO, null, 10L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("sexo de Nina");
    }

    @Test
    void atualizar_recusaVirarMachoQuemJaEMae() {
        Animal nina = Animal.builder().id(10L).nome("Nina").tutor(Tutor.builder().id(1L).nome("João").build())
                .especie(Especie.CAO).sexo(SexoAnimal.FEMEA).build();
        when(animalRepository.findById(10L)).thenReturn(Optional.of(nina));
        when(animalRepository.existsByMaeId(10L)).thenReturn(true);

        assertThatThrownBy(() -> animalService.atualizar(10L,
                new AnimalRequest("Nina", Especie.CAO, SexoAnimal.MACHO, null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("continuar como fêmea");
        assertThat(nina.getSexo()).isEqualTo(SexoAnimal.FEMEA);
    }

    @Test
    void atualizar_recusaTrocarAEspecieDeQuemJaEMae() {
        Animal nina = Animal.builder().id(10L).nome("Nina").tutor(Tutor.builder().id(1L).nome("João").build())
                .especie(Especie.CAO).sexo(SexoAnimal.FEMEA).build();
        when(animalRepository.findById(10L)).thenReturn(Optional.of(nina));
        when(animalRepository.existsByMaeId(10L)).thenReturn(true);

        assertThatThrownBy(() -> animalService.atualizar(10L,
                new AnimalRequest("Nina", Especie.GATO, SexoAnimal.FEMEA, null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("trocar de espécie");
    }

    @Test
    void atualizar_preencheOSexoDeAnimalAntigo() {
        Animal nina = Animal.builder().id(10L).nome("Nina").tutor(Tutor.builder().id(1L).nome("João").build())
                .especie(Especie.CAO).build();
        when(animalRepository.findById(10L)).thenReturn(Optional.of(nina));

        AnimalResponse resposta = animalService.atualizar(10L,
                new AnimalRequest("Nina", Especie.CAO, SexoAnimal.FEMEA, null, null));

        assertThat(resposta.sexo()).isEqualTo(SexoAnimal.FEMEA);
    }

    @Test
    void listarPorId_deveLancarAnimalNaoEncontrado() {
        Long id = 99L;
        when(animalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.listarPorId(id))
                .isInstanceOf(AnimalNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }
}
