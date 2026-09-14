package com.br.octopus_msmedications.domain;

import com.br.octopus_msmedications.domain.enums.SchemeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_medication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_id", nullable = false)
    private Long animalId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String dosage; // Ex: "500mg", "10ml"

    @Column(name = "hour_frequency")
    private Integer hourFrequency; // Ex: de 8 em 8 horas

    @Enumerated(EnumType.STRING)
    @Column(name = "scheme_type", nullable = false)
    private StraSchemeType schemeType;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(columnDefinition = "TEXT")
    private String instructions; // Ex: "Tomar após as refeições"
}