package com.br.octopus_msmedications.repository;

import com.br.octopus_msmedications.domain.Medicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MedicacaoRepository extends JpaRepository<Medicacao, Long> {

    List<Medicacao> findByFabricanteIgnoreCase(String fabricante);

    List<Medicacao> findByNomeComercialContainingIgnoreCase(String nomeComercial);

    Optional<Medicacao> findByNumeroRegistroAnvisa(String numeroRegistroAnvisa);

    boolean existsByNumeroRegistroAnvisa(String numeroRegistroAnvisa);

    // Baixa só se houver saldo; devolve 0 linhas quando falta estoque (ou o id não existe).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Medicacao m set m.quantidade = m.quantidade - :quantidade where m.id = :id and m.quantidade >= :quantidade")
    int reservar(Long id, int quantidade);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Medicacao m set m.quantidade = m.quantidade + :quantidade where m.id = :id")
    int devolver(Long id, int quantidade);
}
