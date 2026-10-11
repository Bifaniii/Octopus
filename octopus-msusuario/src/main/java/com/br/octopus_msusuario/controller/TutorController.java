package com.br.octopus_msusuario.controller;

import com.br.octopus_msusuario.dto.request.AnimalRequest;
import com.br.octopus_msusuario.dto.request.TutorRequest;
import com.br.octopus_msusuario.dto.response.AnimalResponse;
import com.br.octopus_msusuario.dto.response.TutorResponse;
import com.br.octopus_msusuario.service.AnimalService;
import com.br.octopus_msusuario.service.TutorService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tutores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
@Tag(name = "Tutores")
@SecurityRequirement(name = "bearerAuth")
public class TutorController {

    private final TutorService tutorService;
    private final AnimalService animalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TutorResponse criar(@Valid @RequestBody TutorRequest request) {
        return tutorService.criar(request);
    }

    @GetMapping
    public List<TutorResponse> listar() {
        return tutorService.listar();
    }

    @GetMapping("/{id}")
    public TutorResponse buscar(@PathVariable Long id) {
        return tutorService.buscar(id);
    }

    // Só os dados do tutor; a lista "animais" do corpo é ignorada aqui (animais têm os próprios endpoints).
    @PutMapping("/{id}")
    public TutorResponse atualizar(@PathVariable Long id, @Valid @RequestBody TutorRequest request) {
        return tutorService.atualizar(id, request);
    }

    @PostMapping("/{id}/animais")
    @ResponseStatus(HttpStatus.CREATED)
    public AnimalResponse adicionarAnimal(@PathVariable Long id, @Valid @RequestBody AnimalRequest request) {
        return animalService.criar(id, request);
    }
}
