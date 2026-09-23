package com.br.octopus_msmedications.domain;

import com.br.octopus_msmedications.domain.enums.TipoEsquema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_medicacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome_comercial", length = 100, nullable = false)
    private String nomeComercial;

    @Column(name = "principio_ativo", length = 200, nullable = false)
    private String principioAtivo;

    @Column(name = "concentracao", length = 50, nullable = false)
    private String concentracao; // Ex.: "500mg", "10ml"

    @Column(name = "forma_farmaceutica", length = 50, nullable = false)
    private String formaFarmaceutica;

    @Column(name = "unidade_medida_embalagem", length = 20, nullable = false)
    private String unidadeMedidaEmbalagem; // mg, mL, g

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_esquema", length = 20, nullable = false)
    private TipoEsquema tipoEsquema;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDateTime dataVencimento;

    @Column(name = "fabricante", length = 255, nullable = false)
    private String fabricante;

    @Column(name = "numero_registro_anvisa", length = 17, nullable = false)
    private String numeroRegistroAnvisa;

    // Simétrica: o serviço grava os dois sentidos do par, então esta coleção já é a lista completa.
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tb_medicacao_interacoes",
            joinColumns = @JoinColumn(name = "medicacao_id"),
            inverseJoinColumns = @JoinColumn(name = "medicacao_proibida_id")
    )
    private Set<Medicacao> interacoesProibidas = new LinkedHashSet<>();

    public void adicionarInteracao(Medicacao outra) {
        interacoesProibidas.add(outra);
        outra.getInteracoesProibidas().add(this);
    }

    public void removerInteracao(Medicacao outra) {
        interacoesProibidas.remove(outra);
        outra.getInteracoesProibidas().remove(this);
    }
}
