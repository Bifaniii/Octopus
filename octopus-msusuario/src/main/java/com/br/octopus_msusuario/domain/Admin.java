package com.br.octopus_msusuario.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", unique = true, nullable = false)
    private Usuario usuario;
}
