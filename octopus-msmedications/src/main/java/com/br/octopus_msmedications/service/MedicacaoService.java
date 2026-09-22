package com.br.octopus_msmedications.service;

import com.br.octopus_medicatoins.domain.Medicacao;
import com.br.octopus_medications.domain.dto.MedicacaoResponse;
import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.repository.MedicacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicacaoService {

    private final MedicacaoRepository repository;

    public MedicacaoService(MedicacaoRepository repository) {
        this.repository = repository;
    }

    // CREATE (CRIAR)
    @Transactional
    public MedicacaoResponse criar(MedicacaoRequest request) {
        Medicacao medicacao = Medicacao.builder()
            .nomeComercial(request.nomeComercial())
            .principioAtivo(request.principioAtivo())
            .concentracao(request.concentracao())
            .formaFarmaceutica(request.formaFarmaceutica())
            .unidadeMedidaEmbalagem(request.unidadeMedidaEmbalagem())
            .dataVencimento(request.dataVencimento())
            .fabricante(request.fabricante())
            .numeroRegistroAnvisa(request.numeroRegistroAnvisa())
            .build();

        return MedicacaoResponse.from(repository.save(medicacao));

    // LISTAR TODOS (READ)
    @Transactional(readOnly = true)
    public List<MedicacaoResponse> listar() {
        return repository.findAll().stream().map(MedicacaoResponse::from).toList(); 
    }

    // LISTAR POR ID (READ)
    @Transactional(readOnly = true)
    public MedicacaoResponse listarPorId(Long id) {
        Medicacao medicacao = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Medicação com o id" + id + " não encontrada")
        return MedicacaoResponse.from(medicacao);
}

        //LISTAR POR FABRICANTE (READ)
        //LISTAR POR NOMECOMERCIAL (READ)
        //UPDATE (PATCH atualiza algumas informações somente)

