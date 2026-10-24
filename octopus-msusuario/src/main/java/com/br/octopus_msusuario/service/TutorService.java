package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.Tutor;
import com.br.octopus_msusuario.dto.request.MensagemAoTutorRequest;
import com.br.octopus_msusuario.dto.request.TutorRequest;
import com.br.octopus_msusuario.dto.response.TutorResponse;
import com.br.octopus_msusuario.exception.RecursoNaoEncontradoException;
import com.br.octopus_msusuario.exception.RegraNegocioException;
import com.br.octopus_msusuario.mail.EmailService;
import com.br.octopus_msusuario.repository.AnimalRepository;
import com.br.octopus_msusuario.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;
    private final EmailService emailService;

    @Transactional
    public TutorResponse criar(TutorRequest request) {
        Tutor tutor = Tutor.builder()
                .nome(request.nome())
                .endereco(request.endereco())
                .dataNascimento(request.dataNascimento())
                .telefone(request.telefone())
                .email(emailOuNulo(request.email()))
                .build();

        if (request.animais() != null) {
            request.animais().forEach(a -> tutor.adicionarAnimal(a.toEntity(animalRepository.buscarMae(a.maeId()))));
        }

        return TutorResponse.from(tutorRepository.save(tutor));
    }

    @Transactional(readOnly = true)
    public List<TutorResponse> listar() {
        return tutorRepository.findAll().stream().map(TutorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TutorResponse buscar(Long id) {
        return TutorResponse.from(obter(id));
    }

    @Transactional
    public TutorResponse atualizar(Long id, TutorRequest request) {
        Tutor tutor = obter(id);
        tutor.setNome(request.nome());
        tutor.setEndereco(request.endereco());
        tutor.setDataNascimento(request.dataNascimento());
        tutor.setTelefone(request.telefone());
        tutor.setEmail(emailOuNulo(request.email()));
        return TutorResponse.from(tutor);
    }

    /**
     * Manda uma mensagem ao e-mail do tutor (hoje, o resumo de alta). O destinatário é sempre o e-mail
     * cadastrado: o endpoint não aceita endereço no corpo. Sem e-mail cadastrado, 422.
     */
    @Transactional(readOnly = true)
    public String enviarMensagem(Long id, MensagemAoTutorRequest mensagem) {
        Tutor tutor = obter(id);
        if (tutor.getEmail() == null) {
            throw new RegraNegocioException("O tutor " + tutor.getNome() + " não tem e-mail cadastrado.");
        }
        emailService.enviarMensagemAoTutor(tutor.getEmail(), mensagem.assunto(), mensagem.texto());
        return tutor.getEmail();
    }

    private static String emailOuNulo(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    Tutor obter(Long id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tutor não encontrado: " + id));
    }
}
