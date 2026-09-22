package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.Medicacao;
import com.br.octopus_msmedications.domain.dto.response.MedicacaoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface MedicacaoRepository extends JpaRepository<Medicacao, Long> {
    List<MedicacaoResponse> findByFabricanteIgnoreCase(String fabricante);
    Optional<MedicacaoResponse> findByNomeComercialIgnoreCase(String nomeComercial);
}
