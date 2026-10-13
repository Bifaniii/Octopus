package com.br.octopus_msprescricao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

// Um medicamento da prescrição, com o esquema de horários. Os horários são gerados aqui (ver horarios()) e
// enviados ao octopus-msdoses, que guarda uma dose por horário e registra a aplicação.
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

    // Texto livre por enquanto (ex.: "5 mg", "1 comprimido").
    @Column(name = "dosagem", length = 100, nullable = false)
    private String dosagem;

    // Mudança contratada de 01/11: sem horário fixo, só com intervalo mínimo entre as aplicações. Não tem quantidade
    // de doses nem início, e o octopus-msdoses cria a dose quando o auxiliar aplica.
    @Column(name = "se_necessario", nullable = false)
    private boolean seNecessario;

    // No item "se necessário", é o intervalo mínimo entre duas aplicações.
    @Column(name = "intervalo_horas", nullable = false)
    private int intervaloHoras;

    @Column(name = "quantidade_doses")
    private Integer quantidadeDoses;

    // Horário da primeira dose.
    @Column(name = "inicio")
    private LocalDateTime inicio;

    @Column(name = "observacao", length = 500)
    private String observacao;

    /** Horários de aplicação: inicio + n * intervaloHoras, para n de 0 a quantidadeDoses - 1. Vazio no "se necessário". */
    public List<LocalDateTime> horarios() {
        if (seNecessario) {
            return List.of();
        }
        return IntStream.range(0, quantidadeDoses)
                .mapToObj(n -> inicio.plusHours((long) n * intervaloHoras))
                .toList();
    }
}
