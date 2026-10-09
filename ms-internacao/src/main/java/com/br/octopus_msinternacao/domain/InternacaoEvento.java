package com.br.octopus_msinternacao.domain;

import com.br.octopus_msinternacao.domain.enums.StatusInternacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// Linha do histórico da internação. Criada só pela própria Internacao; nunca é alterada nem apagada.
@Entity
@Table(name = "tb_internacao_eventos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class InternacaoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internacao_id", nullable = false)
    private Internacao internacao;

    // Nulo no evento de admissão.
    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusInternacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 30, nullable = false)
    private StatusInternacao statusNovo;

    @Column(name = "baia_id", nullable = false)
    private UUID baiaId;

    @Column(name = "usuario", nullable = false)
    private String usuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
}
