package com.ocptopus.ms_cadastro_baias.domain;


import com.ocptopus.ms_cadastro_baias.domain.ENUM.Tipo;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "cadastro_baia")
public class Cadastro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "tipo")
    private Tipo tipo;

    @Column(name = "tamanho")
    @Max(value = 6, message = "O total de animais não pode passar de 6")
    private int tamanho;

    @Column(name = "quantidade_animais")
    private int quantidade;

    @Column(name = "nome", length = 25)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "livre")
    private Boolean livre = Boolean.TRUE;

}
