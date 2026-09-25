package com.br.octopus_msusuario.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// Stub mínimo: o cadastro completo de Animal (espécie, vacina antirrábica etc.) é outra tela do TAP.
@Entity
@Table(name = "tb_animais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "especie", nullable = false)
    private String especie;

    @Column(name = "data_ultima_vacina_antirrabica", nullable = true)
    private LocalDateTime dataUltimaAntirrabica;
}
