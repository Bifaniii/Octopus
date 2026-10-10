package com.br.octopus_msprescricao.repository;

import com.br.octopus_msprescricao.domain.Prescricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface PrescricaoRepository extends JpaRepository<Prescricao, Long> {

    List<Prescricao> findByInternacaoIdOrderByCriadoEmDesc(Long internacaoId);

    // RN-03: medicamentos das prescrições ativas da internação, que o animal já está recebendo.
    @Query("""
            select distinct i.medicacaoId from ItemPrescricao i
            where i.prescricao.internacaoId = :internacaoId and i.prescricao.ativo = true
            """)
    List<Long> medicacoesEmUso(Long internacaoId);
}
