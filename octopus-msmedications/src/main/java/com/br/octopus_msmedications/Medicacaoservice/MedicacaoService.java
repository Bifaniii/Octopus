package com.br.octopus_msmedications.service;

import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.repository.MedicacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicacaoService {

    private final MedicacaoRepository repository;

    public MedicacaoService(MedicacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Medicacao criar(Medicacao medicacao) {
        return repository.save(medicacao);
    }

    @Transactional(readOnly = true)
    public List<Medicacao> listar(String fabricante) {
        if (fabricante == null || fabricante.isBlank()) {
            return repository.findAll();
        }
        return repository.findByFabricanteIgnoreCase(fabricante);
    }

    @Transactional(readOnly = true)
    public Medicacao buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Medicação não encontrada com id " + id));
    }

    @Transactional(readOnly = true)
    public Medicacao buscarPorNomeComercial(String nomeComercial) {
        return repository.findByNomeComercialIgnoreCase(nomeComercial)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Medicação não encontrada: " + nomeComercial));
    }

    @Transactional
    public Medicacao atualizar(Long id, Medicacao dados) {
        Medicacao existente = buscarPorId(id);
        existente.setNomeComercial(dados.getNomeComercial());
        existente.setFabricante(dados.getFabricante());
        // copie aqui os demais campos da sua entidade
        return repository.save(existente);
    }

    @Transactional
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Medicação não encontrada com id " + id);
        }
        repository.deleteById(id);
    }
}
