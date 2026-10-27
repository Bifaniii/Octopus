package com.br.octopus_msdoses.repository;

import com.br.octopus_msdoses.domain.ItemSeNecessario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemSeNecessarioRepository extends JpaRepository<ItemSeNecessario, Long> {

    Optional<ItemSeNecessario> findByItemPrescricaoId(Long itemPrescricaoId);

    // Dois auxiliares aplicando o mesmo item ao mesmo tempo: o segundo espera e então vê a aplicação do primeiro.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemSeNecessario i where i.itemPrescricaoId = :itemPrescricaoId")
    Optional<ItemSeNecessario> travarPorItem(Long itemPrescricaoId);

    List<ItemSeNecessario> findByPrescricaoId(Long prescricaoId);

    boolean existsByPrescricaoIdAndAtivoTrue(Long prescricaoId);

    List<ItemSeNecessario> findByInternacaoId(Long internacaoId);

    // Painel: itens ativos, de uma internação ou de todas.
    @Query("""
            select i from ItemSeNecessario i
            where i.ativo = true and (:internacaoId is null or i.internacaoId = :internacaoId)
            order by i.internacaoId asc, i.id asc
            """)
    List<ItemSeNecessario> ativos(Long internacaoId);
}
