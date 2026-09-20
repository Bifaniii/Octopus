package com.br.octopus_msmedications.service;

import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.repository.MedicationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicationService {

    private final MedicationRepository repository;

    public MedicationService(MedicationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Medicacao create(Medicacao medication) {
        return repository.save(medication);
    }

    @Transactional(readOnly = true)
    public List<Medicacao> list(String manufacturer) {
        if (manufacturer == null || manufacturer.isBlank()) {
            return repository.findAll();
        }
        return repository.findByManufacturerIgnoreCase(manufacturer);
    }

    @Transactional(readOnly = true)
    public Medicacao findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Medication not found with id " + id));
    }

    @Transactional
    public Medicacao update(Long id, Medicacao data) {
        Medicacao existing = findById(id);
        existing.setNomeComercial(data.getNomeComercial());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Medication not found with id " + id);
        }
        repository.deleteById(id);
    }
}