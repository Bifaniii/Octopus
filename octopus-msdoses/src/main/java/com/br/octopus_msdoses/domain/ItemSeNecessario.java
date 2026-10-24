package com.br.octopus_msdoses.domain;

import com.br.octopus_msdoses.domain.enums.TipoEsquema;
import jakarta.persistence.*;
import lombok.*;

// Item de prescrição "se necessário" (mudança contratada de 01/11/2026): sem horário fixo, só com intervalo mínimo
// entre aplicações. Não gera dose pendente nem entra no painel de atrasos; cada aplicação vira uma Dose aplicada.
@Entity
@Table(name = "tb_itens_se_necessario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_itens_se_necessario_item", columnNames = "item_prescricao_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSeNecessario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_prescricao_id", nullable = false)
    private Long itemPrescricaoId;

    @Column(name = "prescricao_id", nullable = false)
    private Long prescricaoId;

    @Column(name = "internacao_id", nullable = false)
    private Long internacaoId;

    @Column(name = "medicacao_id", nullable = false)
    private Long medicacaoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "esquema", length = 20, nullable = false)
    private TipoEsquema esquema;

    @Column(name = "intervalo_minimo_horas", nullable = false)
    private int intervaloMinimoHoras;

    // false quando a prescrição é desativada ou o animal sai.
    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    public static ItemSeNecessario registrar(Long itemPrescricaoId, Long prescricaoId, Long internacaoId,
                                             Long medicacaoId, TipoEsquema esquema, int intervaloMinimoHoras) {
        ItemSeNecessario item = new ItemSeNecessario();
        item.itemPrescricaoId = itemPrescricaoId;
        item.prescricaoId = prescricaoId;
        item.internacaoId = internacaoId;
        item.medicacaoId = medicacaoId;
        item.esquema = esquema;
        item.intervaloMinimoHoras = intervaloMinimoHoras;
        item.ativo = true;
        return item;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }
}
