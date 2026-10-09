package com.br.octopus_msprescricao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// Um medicamento da prescrição, com o esquema de horários. As doses (um horário por aplicação) são geradas por
// outro módulo a partir daqui: inicio + n * intervaloHoras, para n de 0 a quantidadeDoses - 1.
@Entity
@Table(name = "tb_itens_prescricao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPrescricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescricao_id", nullable = false)
    private Prescricao prescricao;

    // Id do medicamento (octopus-msmedications, que usa UUID). Sem @ManyToOne e sem FK: tabela de outro módulo.
    @Column(name = "medicacao_id", nullable = false)
    private UUID medicacaoId;

    // Texto livre por enquanto (ex.: "5 mg", "1 comprimido").
    @Column(name = "dosagem", length = 100, nullable = false)
    private String dosagem;

    @Column(name = "intervalo_horas", nullable = false)
    private int intervaloHoras;

    @Column(name = "quantidade_doses", nullable = false)
    private int quantidadeDoses;

    // Horário da primeira dose.
    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "observacao", length = 500)
    private String observacao;
}
