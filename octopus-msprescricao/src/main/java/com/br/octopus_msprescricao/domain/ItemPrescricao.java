package com.br.octopus_msprescricao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    // Id do medicamento (octopus-msmedications). Sem @ManyToOne e sem FK: a tabela é de outro módulo.
    @Column(name = "medicacao_id", nullable = false)
    private Long medicacaoId;

    // Texto livre por enquanto (ex.: "5 mg", "1 comprimido").
    @Column(name = "dosagem", length = 100, nullable = false)
    private String dosagem;

    @Column(name = "intervalo_horas", nullable = false)
    private int intervaloHoras;

    @Column(name = "quantidade_doses", nullable = false)
    private int quantidadeDoses;

    @Builder.Default
    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio = LocalDateTime.now();
 
}
