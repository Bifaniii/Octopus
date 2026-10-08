package com.br.octopus_msprescricao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_itens_prescricao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPrescricao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescricao_id", nullable = false)
    private Prescricao prescricao;

    // Id do medicamento (octopus-msmedications). Sem @ManyToOne e sem FK: a tabela é de outro módulo.
    @Column(name = "medicacao_id", nullable = false)
    private UUID medicacaoId;

    // Texto livre por enquanto (ex.: "5 mg", "1 comprimido").
    @Column(name = "dosagem", length = 100, nullable = false)
    private String dosagem;

    @Column(name = "intervalo_horas", nullable = false)
    private int intervaloHoras;

    @Column(name = "quantidade_doses", nullable = false)
    private int quantidadeDoses;

    // Horário da primeira dose. As demais saem de inicio + n * intervaloHoras.
    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Builder.Default
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("horarioPrevisto ASC")
    private List<Dose> doses = new ArrayList<>();

    public void adicionarDose(Dose dose) {
        dose.setItem(this);
        doses.add(dose);
    }

    /** Gera um horário por dose, a partir de {@code inicio}, espaçados por {@code intervaloHoras}. */
    public void gerarDoses() {
        doses.clear();
        for (int i = 0; i < quantidadeDoses; i++) {
            adicionarDose(Dose.builder()
                    .horarioPrevisto(inicio.plusHours((long) i * intervaloHoras))
                    .build());
        }
    }
}
