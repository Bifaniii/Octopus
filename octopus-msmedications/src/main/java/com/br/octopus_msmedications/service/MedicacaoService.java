package com.br.octopus_msmedications.service;


import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.domain.dto.request.MedicacaoResquestUpdate;
import com.br.octopus_msmedications.repository.MedicacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.br.octopus_msmedications.domain.dto.response.MedicacaoResponse;

import java.util.Collections;
import java.util.List;
import  com.br.octopus_msmedications.domain.dto.request.MedicacaoRequest;

@Service
@RequiredArgsConstructor
public class MedicacaoService {

    private final MedicacaoRepository repository;

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
    }

    // LISTAR TODOS (READ)
    @Transactional(readOnly = true)
    public List<MedicacaoResponse> listar() {
        return repository.findAll().stream().map(MedicacaoResponse::from).toList();
    }

    // LISTAR POR ID (READ)
    @Transactional(readOnly = true)
    public MedicacaoResponse listarPorId(Long id) {
        Medicacao medicacao = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicação com o id" + id + " não encontrada"));
        return MedicacaoResponse.from(medicacao);
    }

    //LISTAR POR FABRICANTE (READ)

    @Transactional(readOnly = true)
    public List<MedicacaoResponse> ListarPorFabricante(String fabricante) {
        return repository.findByFabricanteIgnoreCase(fabricante);
    }

    //LISTAR POR NOMECOMERCIAL (READ)
    @Transactional(readOnly = true)
    public List<MedicacaoResponse> ListarPorNomeComercial(String nomeComercial) {
        return repository.findByNomeComercialIgnoreCase(nomeComercial);
    }

    //UPDATE (PATCH atualiza algumas informações somente)
    @Transactional
    public MedicacaoResponse atualiarParcial(Long id, MedicacaoResquestUpdate resquest) {
        Medicacao medicacao = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicação com o id" + id + "não encontrada"));
        if (resquest.nomeComercial() != null && !resquest.nomeComercial().isBlank()){
            medicacao.setNomeComercial(resquest.nomeComercial());
        }
        if (resquest.principioAtivo() !=null && !resquest.principioAtivo().isBlank()){
            medicacao.setPrincipioAtivo(resquest.principioAtivo());
        }
        if (resquest.concentracao() !=null && !resquest.concentracao().isBlank()){
            medicacao.setConcentracao((resquest.concentracao()));
        }
        if (resquest.formaFarmaceutica() !=null && !resquest.formaFarmaceutica().isBlank()){
            medicacao.setFormaFarmaceutica(resquest.formaFarmaceutica());
        }
        if (resquest.unidadeMedidaEmbalagem() != null && !resquest.unidadeMedidaEmbalagem().isBlank()){
            medicacao.setUnidadeMedidaEmbalagem(resquest.unidadeMedidaEmbalagem());
        }
        if (resquest.dataVencimento() != null){
            medicacao.setDataVencimento(resquest.dataVencimento());
        }
        if(resquest.fabricante() != null && !resquest.fabricante().isBlank()){
            medicacao.setFabricante(resquest.fabricante());
        }
        if (resquest.numeroRegistroAnvisa() != null && !resquest.numeroRegistroAnvisa().isBlank()){
            medicacao.setNumeroRegistroAnvisa(resquest.numeroRegistroAnvisa());
        }
        return MedicacaoResponse.from(repository.save(medicacao));
    }


}




