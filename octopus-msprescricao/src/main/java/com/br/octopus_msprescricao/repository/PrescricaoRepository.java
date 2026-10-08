package com.br.octopus_msprescricao.repository;

import com.br.octopus_msprescricao.domain.Prescricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrescricaoRepository extends JpaRepository<Prescricao, UUID> {

    List<Prescricao> findByInternacaoIdOrderByCriadoEmDesc(UUID internacaoId);
}
