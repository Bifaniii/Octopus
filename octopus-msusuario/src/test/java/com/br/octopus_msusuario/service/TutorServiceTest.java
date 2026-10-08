package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Tutor;
import com.br.octopus_msusuario.dto.request.AnimalRequest;
import com.br.octopus_msusuario.dto.request.TutorRequest;
import com.br.octopus_msusuario.dto.response.TutorResponse;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.repository.AnimalRepository;
import com.br.octopus_msusuario.repository.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorServiceTest {

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private AnimalRepository animalRepository;

    @InjectMocks
    private TutorService tutorService;

    @Test
    void criar_deveVincularOsAnimaisAoTutor() {
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));
        var request = new TutorRequest("João", "Rua A, 10", LocalDate.of(1985, 1, 1), "11988887777",
                List.of(new AnimalRequest("Rex", "Cão", null, null), new AnimalRequest("Mia", "Gato", null, null)));

        TutorResponse response = tutorService.criar(request);

        ArgumentCaptor<Tutor> captor = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).save(captor.capture());
        Tutor salvo = captor.getValue();
        assertThat(salvo.getAnimais()).hasSize(2).allSatisfy(animal -> assertThat(animal.getTutor()).isSameAs(salvo));
        assertThat(salvo.getAnimais()).extracting("especie").containsExactly("Cão", "Gato");
        assertThat(response.animais()).hasSize(2);
    }

    @Test
    void criar_deveAceitarTutorSemAnimais() {
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));
        var request = new TutorRequest("João", "Rua A, 10", LocalDate.of(1985, 1, 1), "11988887777", null);

        assertThat(tutorService.criar(request).animais()).isEmpty();
    }

    @Test
    void buscar_deveLancarExcecaoQuandoTutorNaoExiste() {
        UUID id = UUID.randomUUID();
        when(tutorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorService.buscar(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }
}
