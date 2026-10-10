package com.br.octopus_msprescricao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


// Um medicamento da prescrição, com o intervalo e o início do esquema de horários.
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

    // Id do medicamento (octopus-msmedications). Sem @ManyToOne e sem FK: tabela de outro módulo.
    @Column(name = "medicacao_id", nullable = false)
    private Long medicacaoId;

    @Column(name = "intervalo_horas", nullable = false)
    private int intervaloHoras;

    // Horário de início.
    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "observacao", length = 500)
    private String observacao;
}
