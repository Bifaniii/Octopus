package com.br.octopus_msprescricao.domain;

import com.br.octopus_msprescricao.domain.enums.StatusDose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Horário gerado pela prescrição. Nasce PENDENTE. Registrar aplicação, janela de 30 minutos (RN-04/RN-05)
 * e doses perdidas (RN-07/RN-08) são do painel de doses, Sprint 3, e ainda não existem aqui.
 */
@Entity
@Table(name = "tb_doses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dose {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemPrescricao item;

    @Column(name = "horario_previsto", nullable = false)
    private LocalDateTime horarioPrevisto;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StatusDose status = StatusDose.PENDENTE;
}
