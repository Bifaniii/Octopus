package com.br.octopus_msprescricao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_prescricoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescricao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;

    // Id da internação (octopus-msinternacao). Sem @ManyToOne e sem FK: a tabela é de outro módulo.
    @Column(name = "internacao_id", nullable = false)
    private UUID internacaoId;

    // Quem prescreveu, vindo do claim "sub" do JWT. O token não traz o id do veterinário.
    @Column(name = "veterinario_email", length = 150, nullable = false)
    private String veterinarioEmail;

    @Column(name = "observacao", length = 500)
    private String observacao;

    // Nada é apagado: a prescrição que sai de uso fica com ativo = false (PATCH /{id}/desativar).
    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Builder.Default
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Builder.Default
    @OneToMany(mappedBy = "prescricao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("inicio ASC")
    private List<ItemPrescricao> itens = new ArrayList<>();

    public void adicionarItem(ItemPrescricao item) {
        item.setPrescricao(this);
        itens.add(item);
    }
}
