package com.br.octopus_msusuario.domain;

import jakarta.persistence.*;
import lombok.*;

import com.br.octopus_msusuario.domain.enums.Especie;
import com.br.octopus_msusuario.domain.enums.SexoAnimal;

import java.time.LocalDate;

@Entity
@Table(name = "tb_animais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    // Anulável no banco só por causa da carga: o animal sem espécie fica nulo (V9) e o ms-internacao o recusa.
    // No cadastro e na edição a espécie é obrigatória.
    @Enumerated(EnumType.STRING)
    @Column(name = "especie", length = 250)
    private Especie especie;

    // Anulável para os animais cadastrados antes de 07/11/2026; obrigatório no cadastro e na edição.
    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", length = 10)
    private SexoAnimal sexo;

    @Column(name = "data_ultima_vacina_antirrabica", nullable = true)
    private LocalDate dataUltimaAntirrabica;

    // Mãe do animal, quando conhecida. O ms-internacao usa na RN-01 (ninhada só com filhotes da mesma mãe).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mae_id")
    private Animal mae;
}
