package com.br.octopus_msusuario.service;

import com.br.octopus_msusuario.domain.enums.Role;
import com.br.octopus_msusuario.dto.response.UsuarioResumoResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsuarioConsultaService {

    // Cada perfil é uma tabela com a sua FK para tb_usuarios, então a consulta roda uma vez por perfil.
    private static final Map<Role, String> ENTIDADE_POR_ROLE = Map.of(
            Role.ROLE_ADMIN, "Admin",
            Role.ROLE_VETERINARIO, "Veterinario",
            Role.ROLE_AUXILIAR, "Auxiliar",
            Role.ROLE_RECEPCIONISTA, "Recepcionista"
    );

    private static final String CONSULTA = """
            select new com.br.octopus_msusuario.dto.response.UsuarioResumoResponse(
                u.id, p.id, p.nome, u.email, u.role, u.ativo, u.criadoEm)
            from %s p join p.usuario u
            where (:nome is null or lower(p.nome) like :nome)
              and (:ativo is null or u.ativo = :ativo)
              and u.criadoEm >= :de and u.criadoEm < :ate
            """;

    private final EntityManager entityManager;

    /**
     * Todos os filtros são opcionais. {@code nome} casa com qualquer trecho do nome, sem diferenciar maiúscula;
     * o período compara a data de cadastro da conta, com {@code ate} inclusivo.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResumoResponse> listar(String nome, Role perfil, Boolean ativo, LocalDate de, LocalDate ate) {
        String padraoNome = nome == null || nome.isBlank() ? null : "%" + nome.trim().toLowerCase() + "%";
        LocalDateTime inicio = de == null ? LocalDateTime.of(1900, 1, 1, 0, 0) : de.atStartOfDay();
        LocalDateTime fim = ate == null ? LocalDateTime.of(9999, 1, 1, 0, 0) : ate.plusDays(1).atStartOfDay();

        List<UsuarioResumoResponse> resultado = new ArrayList<>();
        ENTIDADE_POR_ROLE.forEach((role, entidade) -> {
            if (perfil == null || perfil == role) {
                resultado.addAll(entityManager
                        .createQuery(CONSULTA.formatted(entidade), UsuarioResumoResponse.class)
                        .setParameter("nome", padraoNome)
                        .setParameter("ativo", ativo)
                        .setParameter("de", inicio)
                        .setParameter("ate", fim)
                        .getResultList());
            }
        });
        resultado.sort(Comparator.comparing(UsuarioResumoResponse::nome, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }
}
