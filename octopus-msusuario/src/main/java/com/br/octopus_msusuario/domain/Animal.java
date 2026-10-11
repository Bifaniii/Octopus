package com.br.octopus_msusuario.domain;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "especie", nullable = false)
    private String especie;

    @Column(name = "data_ultima_vacina_antirrabica", nullable = true)
    private LocalDate dataUltimaAntirrabica;

    // Mãe do animal, quando conhecida. O ms-internacao usa na RN-01 (ninhada só com filhotes da mesma mãe).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mae_id")
    private Animal mae;
}
