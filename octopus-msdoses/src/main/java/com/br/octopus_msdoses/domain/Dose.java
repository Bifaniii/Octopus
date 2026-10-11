package com.br.octopus_msdoses.domain;

import com.br.octopus_msdoses.domain.enums.StatusDose;
import com.br.octopus_msdoses.exception.RegraDeNegocioException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Um horário previsto de aplicação, gerado a partir de um item da prescrição. Sem @Setter: o status só muda por
// aplicar() e cancelar(), e a aplicação registrada não é mais alterada (RN-05).
@Entity
@Table(name = "tb_doses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_doses_item_horario", columnNames = {"item_prescricao_id", "horario_previsto"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ids de outros módulos (msprescricao, ms-internacao, msmedications): sem @ManyToOne e sem FK.
    @Column(name = "prescricao_id", nullable = false)
    private Long prescricaoId;

    @Column(name = "item_prescricao_id", nullable = false)
    private Long itemPrescricaoId;

    @Column(name = "internacao_id", nullable = false)
    private Long internacaoId;

    @Column(name = "medicacao_id", nullable = false)
    private Long medicacaoId;

    @Column(name = "horario_previsto", nullable = false)
    private LocalDateTime horarioPrevisto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StatusDose status;

    @Column(name = "aplicada_em")
    private LocalDateTime aplicadaEm;

    // E-mail do auxiliar, vindo do claim "sub" do token.
    @Column(name = "aplicada_por", length = 150)
    private String aplicadaPor;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    public static Dose agendar(Long prescricaoId, Long itemPrescricaoId, Long internacaoId, Long medicacaoId,
                               LocalDateTime horarioPrevisto) {
        Dose dose = new Dose();
        dose.prescricaoId = prescricaoId;
        dose.itemPrescricaoId = itemPrescricaoId;
        dose.internacaoId = internacaoId;
        dose.medicacaoId = medicacaoId;
        dose.horarioPrevisto = horarioPrevisto;
        dose.status = StatusDose.PENDENTE;
        return dose;
    }

    // RN-05: registra quem aplicou e quando, uma vez só.
    public void aplicar(String auxiliar, LocalDateTime agora, String observacao) {
        if (status == StatusDose.APLICADA) {
            throw new RegraDeNegocioException("RN-05: a aplicação desta dose já foi registrada por " + aplicadaPor
                    + " e não pode ser alterada.");
        }
        if (status != StatusDose.PENDENTE) {
            throw new RegraDeNegocioException("A dose está " + status + " e não pode ser aplicada.");
        }
        this.status = StatusDose.APLICADA;
        this.aplicadaPor = auxiliar;
        this.aplicadaEm = agora;
        this.observacao = observacao;
    }

    /** Cancela se ainda estiver pendente. Devolve se mudou alguma coisa. */
    public boolean cancelar() {
        if (status != StatusDose.PENDENTE) {
            return false;
        }
        this.status = StatusDose.CANCELADA;
        return true;
    }
}
