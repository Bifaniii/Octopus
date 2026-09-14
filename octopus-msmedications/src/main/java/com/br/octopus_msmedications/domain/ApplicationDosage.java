package com.br.octopus_msmedications.domain;

import com.br.octopus_msmedications.domain.enums.StatusDosage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_application_dosage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDosage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(name = "scheduled_time", nullable = false) // Horário Previsto
    private LocalDateTime scheduledTime;

    @Column(name = "application_time")
    private LocalDateTime applicationTime; // nulo até ser registrada

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDosage status; // Status possíveis: PENDING, APPLIED, LOST, REAPPLIED, SKIPPED

    @Column(name = "resp_auxiliary")
    private String respAuxiliary;

    @Column(name = "register_date", updatable = false)
    private LocalDateTime registerDate; // RN-05: não editável depois de criada
}
