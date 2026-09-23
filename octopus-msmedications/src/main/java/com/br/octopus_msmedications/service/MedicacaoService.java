package com.br.octopus_msmedications.service;

import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.dto.request.MedicacaoRequest;
import com.br.octopus_msmedications.dto.request.MedicacaoUpdateRequest;
import com.br.octopus_msmedications.dto.response.MedicacaoResponse;
import com.br.octopus_msmedications.exception.RecursoDuplicadoException;
import com.br.octopus_msmedications.exception.RecursoNaoEncontradoException;
import com.br.octopus_msmedications.exception.RegraNegocioException;
import com.br.octopus_msmedications.repository.MedicacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicacaoService {

    private final MedicacaoRepository repository;

    @Transactional
    public MedicacaoResponse criar(MedicacaoRequest request) {
        if (repository.existsByNumeroRegistroAnvisa(request.numeroRegistroAnvisa())) {
            throw new RecursoDuplicadoException("Registro ANVISA já cadastrado: " + request.numeroRegistroAnvisa());
        }

        Medicacao medicacao = Medicacao.builder()
                .nomeComercial(request.nomeComercial())
                .principioAtivo(request.principioAtivo())
                .concentracao(request.concentracao())
                .formaFarmaceutica(request.formaFarmaceutica())
                .unidadeMedidaEmbalagem(request.unidadeMedidaEmbalagem())
                .tipoEsquema(request.tipoEsquema())
                .dataVencimento(request.dataVencimento())
                .fabricante(request.fabricante())
                .numeroRegistroAnvisa(request.numeroRegistroAnvisa())
                .build();

        // Salva antes de ligar as interações: o par precisa dos dois lados já persistidos.
        Medicacao salva = repository.save(medicacao);
        definirInteracoes(salva, request.interacoesProibidas());
        return MedicacaoResponse.from(salva);
    }

    @Transactional(readOnly = true)
    public List<MedicacaoResponse> listar() {
        return repository.findAll().stream().map(MedicacaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MedicacaoResponse buscar(UUID id) {
        return MedicacaoResponse.from(obter(id));
    }

    @Transactional(readOnly = true)
    public List<MedicacaoResponse> listarPorFabricante(String fabricante) {
        return repository.findByFabricanteIgnoreCase(fabricante).stream().map(MedicacaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicacaoResponse> listarPorNomeComercial(String nomeComercial) {
        return repository.findByNomeComercialContainingIgnoreCase(nomeComercial).stream()
                .map(MedicacaoResponse::from)
                .toList();
    }

    @Transactional
    public MedicacaoResponse atualizarParcial(UUID id, MedicacaoUpdateRequest request) {
        Medicacao medicacao = obter(id);

        if (preenchido(request.nomeComercial())) {
            medicacao.setNomeComercial(request.nomeComercial());
        }
        if (preenchido(request.principioAtivo())) {
            medicacao.setPrincipioAtivo(request.principioAtivo());
        }
        if (preenchido(request.concentracao())) {
            medicacao.setConcentracao(request.concentracao());
        }
        if (preenchido(request.formaFarmaceutica())) {
            medicacao.setFormaFarmaceutica(request.formaFarmaceutica());
        }
        if (preenchido(request.unidadeMedidaEmbalagem())) {
            medicacao.setUnidadeMedidaEmbalagem(request.unidadeMedidaEmbalagem());
        }
        if (request.tipoEsquema() != null) {
            medicacao.setTipoEsquema(request.tipoEsquema());
        }
        if (request.dataVencimento() != null) {
            medicacao.setDataVencimento(request.dataVencimento());
        }
        if (preenchido(request.fabricante())) {
            medicacao.setFabricante(request.fabricante());
        }
        if (preenchido(request.numeroRegistroAnvisa())
                && !request.numeroRegistroAnvisa().equals(medicacao.getNumeroRegistroAnvisa())) {
            if (repository.existsByNumeroRegistroAnvisa(request.numeroRegistroAnvisa())) {
                throw new RecursoDuplicadoException("Registro ANVISA já cadastrado: " + request.numeroRegistroAnvisa());
            }
            medicacao.setNumeroRegistroAnvisa(request.numeroRegistroAnvisa());
        }
        if (request.interacoesProibidas() != null) {
            definirInteracoes(medicacao, request.interacoesProibidas());
        }

        return MedicacaoResponse.from(medicacao);
    }

    @Transactional
    public void remover(UUID id) {
        Medicacao medicacao = obter(id);
        // Desfaz os dois sentidos antes de apagar, senão a FK das interações barra o delete.
        definirInteracoes(medicacao, Set.of());
        repository.delete(medicacao);
    }

    // Substitui a lista de interações, mantendo a simetria dos pares.
    private void definirInteracoes(Medicacao medicacao, Set<UUID> idsDesejados) {
        Set<UUID> ids = idsDesejados == null ? Set.of() : idsDesejados;

        if (ids.contains(medicacao.getId())) {
            throw new RegraNegocioException("Um medicamento não pode ter interação proibida consigo mesmo.");
        }

        for (Medicacao atual : new LinkedHashSet<>(medicacao.getInteracoesProibidas())) {
            if (!ids.contains(atual.getId())) {
                medicacao.removerInteracao(atual);
            }
        }

        for (UUID idProibido : ids) {
            Medicacao proibida = repository.findById(idProibido)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Medicação informada como interação proibida não encontrada: " + idProibido));
            medicacao.adicionarInteracao(proibida);
        }
    }

    private Medicacao obter(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Medicação não encontrada: " + id));
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
