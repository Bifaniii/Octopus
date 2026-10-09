package com.br.octopus_msinternacao.repository;

import com.br.octopus_msinternacao.domain.Internacao;
import com.br.octopus_msinternacao.domain.enums.StatusInternacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InternacaoRepository extends JpaRepository<Internacao, Long> {

    // RN-01: trava as internações abertas da baia até o fim da transação. No MySQL (REPEATABLE READ), com o
    // índice (baia_id, status), o FOR UPDATE também trava a faixa da baia, então outra admissão concorrente
    // espera (ou perde por deadlock) em vez de ocupar a mesma vaga. Devolve a lista, e não um count, porque a
    // regra da ninhada precisa da mãe de cada ocupante.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Internacao> findByBaiaIdAndStatusNot(UUID baiaId, StatusInternacao status);

    boolean existsByAnimalIdAndStatusNot(UUID animalId, StatusInternacao status);

    // Filtros opcionais: parâmetro nulo = não filtra.
    @Query("""
            select i from Internacao i
            where (:status is null or i.status = :status)
              and (:baiaId is null or i.baiaId = :baiaId)
              and (:animalId is null or i.animalId = :animalId)
            order by i.dataAdmissao desc
            """)
    List<Internacao> listar(StatusInternacao status, UUID baiaId, UUID animalId);
}
