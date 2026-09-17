package com.br.octopus_msmedications.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_medicacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nomeComercial;

    @Column(nullable = false, length = 200)
    private String principioAtivo;

    @Column(nullable = false, length = 50)
    private String concentracao; // Ex: "500mg", "10ml"

    @Column(nullable = false, length = 50)
    private String formaFarmaceutica; // Ex.: Xarope, Gotas, Pomada...

    @Column(nullable = false)
    private String unidadeMedidaEmbalagem; // Ex.: mg, mL, g...

    @Column(nullable = false, name = "data_vencimento")
    private LocalDateTime dataVencimento;;

    @Column(nullable = false)
    private String fabricante;

    @Column(nullable = false, name = "numero_registro_anvisa")
    private String numeroRegistroAnvisa;
}
