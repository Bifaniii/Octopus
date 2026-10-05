package com.br.octopus_msbaias.service;

import com.br.octopus_msbaias.domain.Baia;
import com.br.octopus_msbaias.dto.request.BaiaRequest;
import com.br.octopus_msbaias.dto.response.BaiaResponse;
import com.br.octopus_msbaias.exception.RecursoDuplicadoException;
import com.br.octopus_msbaias.exception.RecursoNaoEncontradoException;
import com.br.octopus_msbaias.exception.RegraNegocioException;
import com.br.octopus_msbaias.repository.BaiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BaiaService {

    // A clínica VidaPet tem 12 baias (ver TAP): não é possível cadastrar mais que isso.
    private static final long LIMITE_BAIAS = 12;

    private final BaiaRepository baiaRepository;

    @Transactional
    public BaiaResponse criar(BaiaRequest request) {
        if (baiaRepository.count() >= LIMITE_BAIAS) {
            throw new RegraNegocioException("Limite de " + LIMITE_BAIAS + " baias atingido; não é possível cadastrar mais.");
        }
        if (baiaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new RecursoDuplicadoException("Já existe uma baia com o nome '" + request.nome() + "'");
        }
        validarCapacidade(request);
        Baia baia = Baia.builder()
                .tipo(request.tipo())
                .nome(request.nome())
                .descricao(request.descricao())
                .capacidade(request.capacidade())
                .build();
        return BaiaResponse.from(baiaRepository.save(baia));
    }

    @Transactional(readOnly = true)
    public List<BaiaResponse> listar() {
        return baiaRepository.findAll().stream().map(BaiaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BaiaResponse buscar(UUID id) {
        return BaiaResponse.from(buscarEntidade(id));
    }

    @Transactional
    public BaiaResponse atualizar(UUID id, BaiaRequest request) {
        Baia baia = buscarEntidade(id);
        if (baiaRepository.existsByNomeIgnoreCaseAndIdNot(request.nome(), id)) {
            throw new RecursoDuplicadoException("Já existe uma baia com o nome '" + request.nome() + "'");
        }
        validarCapacidade(request);
        baia.setTipo(request.tipo());
        baia.setNome(request.nome());
        baia.setDescricao(request.descricao());
        baia.setCapacidade(request.capacidade());
        return BaiaResponse.from(baiaRepository.save(baia));
    }

    @Transactional
    public void remover(UUID id) {
        baiaRepository.delete(buscarEntidade(id));
    }

    private Baia buscarEntidade(UUID id) {
        return baiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Baia não encontrada: " + id));
    }

    // A capacidade informada não pode ultrapassar o máximo do tipo (coletiva/ninhada: 6; isolamento: 1).
    private void validarCapacidade(BaiaRequest request) {
        int maxima = request.tipo().getCapacidadeMaxima();
        if (request.capacidade() > maxima) {
            throw new RegraNegocioException(
                    "Capacidade " + request.capacidade() + " excede o máximo do tipo " + request.tipo() + " (" + maxima + ").");
        }
    }
}
