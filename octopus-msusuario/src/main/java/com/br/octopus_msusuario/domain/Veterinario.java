package com.br.octopus_msusuario.domain;

import com.br.octopus_msusuario.domain.enums.Especializacao;
import com.br.octopus_msusuario.domain.enums.TipoPessoa;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_veterinarios", uniqueConstraints = {
        // O número do CRMV só é único dentro da UF: o mesmo número pode existir em estados diferentes.
        @UniqueConstraint(name = "uk_veterinarios_crmv", columnNames = {"crmv", "crmv_uf"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Veterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @Column(name = "cpf_cnpj", length = 14, unique = true, nullable = false)
    private String cpfCnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false)
    private TipoPessoa tipoPessoa;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "telefone", length = 20, nullable = false)
    private String telefone;

    @Column(name = "crmv", length = 20, nullable = false)
    private String crmv;

    @Column(name = "crmv_uf", length = 2, nullable = false)
    private String crmvUf;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "especializacao", length = 30, nullable = false)
    private Especializacao especializacao = Especializacao.GERAL;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", unique = true, nullable = false)
    private Usuario usuario;
}
