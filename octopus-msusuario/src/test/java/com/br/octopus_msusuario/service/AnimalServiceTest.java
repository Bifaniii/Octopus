package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Animal;
import com.br.octopus_msusuario.domain.Tutor;
import com.br.octopus_msusuario.dto.request.AnimalRequest;
import com.br.octopus_msusuario.dto.response.AnimalResponse;
import com.br.octopus_msusuario.exception.AnimalNaoEncontradoException;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

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
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = Tutor.builder().id(tutorId).nome("João").build();
        when(tutorService.obter(tutorId)).thenReturn(tutor);
        when(animalRepository.save(any(Animal.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate vacina = LocalDate.of(2026, 3, 1);

        AnimalResponse response = animalService.criar(tutorId, new AnimalRequest("Rex", "Cão", vacina));

        ArgumentCaptor<Animal> captor = ArgumentCaptor.forClass(Animal.class);
        verify(animalRepository).save(captor.capture());
        assertThat(captor.getValue().getEspecie()).isEqualTo("Cão");
        assertThat(captor.getValue().getDataUltimaAntirrabica()).isEqualTo(vacina);
        assertThat(response.tutorId()).isEqualTo(tutorId);
        assertThat(tutor.getAnimais()).containsExactly(captor.getValue());
    }

    @Test
    void criar_naoDeveSalvarQuandoTutorNaoExiste() {
        UUID tutorId = UUID.randomUUID();
        when(tutorService.obter(tutorId)).thenThrow(new RecursoNaoEncontradoException("Tutor não encontrado: " + tutorId));

        assertThatThrownBy(() -> animalService.criar(tutorId, new AnimalRequest("Rex", "Cão", null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(animalRepository, never()).save(any());
    }

    @Test
    void listarPorId_deveLancarAnimalNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(animalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.listarPorId(id))
                .isInstanceOf(AnimalNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }
}
