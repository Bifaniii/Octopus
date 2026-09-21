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

    public List<> getAll() {
        return 
    }
}
