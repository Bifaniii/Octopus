package com.br.octopus_msbaias.domain;

import com.br.octopus_msbaias.domain.enums.Tipo;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_baias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Baia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private Tipo tipo;

    @Column(name = "nome", length = 25, nullable = false, unique = true)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "capacidade", nullable = false)
    private int capacidade;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
}
