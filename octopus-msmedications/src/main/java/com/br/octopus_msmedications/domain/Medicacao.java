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
    private String nomeComercial; // Ex.: Novalgina

    @Column(name = "principio_ativo", length = 200, nullable = false)
    private String principioAtivo; // Ex.: Dipirona monoidratada

    @Column(name = "concentracao", length = 50, nullable = false)
    private String concentracao; // Ex.: "500mg", "10ml"

    @Column(name = "forma_farmaceutica", length = 50, nullable = false)
    private String formaFarmaceutica; // Ex.: Xarope, Gotas, Pomada...

    @Column(name = "unidade_medida_embalagem", length = 20, nullable = false)
    private String unidadeMedidaEmbalagem; // Ex.: mg, mL, g...

    // Contínuo ou sintomático (item 3 do escopo do TAP).
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_esquema", length = 20, nullable = false)
    private TipoEsquema tipoEsquema;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDateTime dataVencimento;

    @Column(name = "fabricante", length = 255, nullable = false)
    private String fabricante;

    @Column(name = "numero_registro_anvisa", length = 17, nullable = false)
    private String numeroRegistroAnvisa;

    // Interações proibidas (item 3 do escopo). A relação é simétrica: se A não pode com B, B não pode
    // com A. O serviço grava os dois sentidos, então basta ler esta coleção para ter a lista completa.
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
