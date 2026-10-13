package com.br.octopus_msdoses.domain;

import com.br.octopus_msdoses.domain.enums.StatusDose;
import com.br.octopus_msdoses.domain.enums.TipoEsquema;
import com.br.octopus_msdoses.exception.RegraDeNegocioException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Um horário previsto de aplicação, gerado a partir de um item da prescrição. Sem @Setter: o status só muda por
// aplicar(), cancelar() e marcarPerdida(), e a aplicação registrada não é mais alterada (RN-05).
@Entity
@Table(name = "tb_doses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_doses_item_horario", columnNames = {"item_prescricao_id", "horario_previsto"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dose {

    /** RN-03: a aplicação vale até 30 minutos antes ou depois do horário previsto. */
    public static final Duration JANELA = Duration.ofMinutes(30);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

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

    @Enumerated(EnumType.STRING)
    @Column(name = "esquema", length = 20, nullable = false)
    private TipoEsquema esquema;

    // Dose "se necessário" (mudança contratada de 01/11): nasce já aplicada, sem horário fixo.
    @Column(name = "se_necessario", nullable = false)
    private boolean seNecessario;

    // RN-07: id da dose perdida que esta reaplica.
    @Column(name = "reaplicacao_de")
    private Long reaplicacaoDe;

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
                               TipoEsquema esquema, LocalDateTime horarioPrevisto) {
        Dose dose = new Dose();
        dose.prescricaoId = prescricaoId;
        dose.itemPrescricaoId = itemPrescricaoId;
        dose.internacaoId = internacaoId;
        dose.medicacaoId = medicacaoId;
        dose.esquema = esquema;
        dose.horarioPrevisto = horarioPrevisto;
        dose.status = StatusDose.PENDENTE;
        return dose;
    }

    /** RN-07: nova dose pendente no lugar da perdida, no horário em que a perda foi constatada. */
    public static Dose reaplicar(Dose perdida, LocalDateTime horario) {
        Dose dose = agendar(perdida.prescricaoId, perdida.itemPrescricaoId, perdida.internacaoId, perdida.medicacaoId,
                perdida.esquema, horario);
        dose.reaplicacaoDe = perdida.id;
        return dose;
    }

    /** Aplicação de um item "se necessário": a dose nasce aplicada, com o horário da própria aplicação. */
    public static Dose aplicadaSeNecessario(ItemSeNecessario item, String auxiliar, LocalDateTime agora,
                                            String observacao) {
        Dose dose = agendar(item.getPrescricaoId(), item.getItemPrescricaoId(), item.getInternacaoId(),
                item.getMedicacaoId(), item.getEsquema(), agora);
        dose.seNecessario = true;
        dose.status = StatusDose.APLICADA;
        dose.aplicadaPor = auxiliar;
        dose.aplicadaEm = agora;
        dose.observacao = observacao;
        return dose;
    }

    public LocalDateTime inicioDaJanela() {
        return horarioPrevisto.minus(JANELA);
    }

    public LocalDateTime fimDaJanela() {
        return horarioPrevisto.plus(JANELA);
    }

    /** RN-04: pendente e a janela já fechou. O limite é inclusivo: às 14h30 a dose das 14h ainda vale. */
    public boolean passouDaJanela(LocalDateTime agora) {
        return status == StatusDose.PENDENTE && agora.isAfter(fimDaJanela());
    }

    // RN-03 e RN-05: registra quem aplicou e quando, uma vez só e dentro da janela.
    public void aplicar(String auxiliar, LocalDateTime agora, String observacao) {
        if (status == StatusDose.APLICADA) {
            throw new RegraDeNegocioException("RN-05: a aplicação desta dose já foi registrada por " + aplicadaPor
                    + " e não pode ser alterada.");
        }
        if (status == StatusDose.PERDIDA) {
            throw new RegraDeNegocioException("RN-04: a dose de " + horarioPrevisto.format(HORA)
                    + " foi perdida e não pode mais ser aplicada.");
        }
        if (status != StatusDose.PENDENTE) {
            throw new RegraDeNegocioException("A dose está " + status + " e não pode ser aplicada.");
        }
        if (agora.isBefore(inicioDaJanela()) || agora.isAfter(fimDaJanela())) {
            throw new RegraDeNegocioException("RN-03: a dose de " + horarioPrevisto.format(HORA)
                    + " só pode ser registrada entre " + inicioDaJanela().format(HORA) + " e "
                    + fimDaJanela().format(HORA) + ".");
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

    // RN-04. Quem decide se a janela fechou é a Varredura, que também aplica a RN-07.
    public void marcarPerdida() {
        if (status != StatusDose.PENDENTE) {
            throw new IllegalStateException("Só dose pendente pode ser perdida: dose " + id + " está " + status);
        }
        this.status = StatusDose.PERDIDA;
    }

    // RN-07, esquema contínuo: as seguintes andam junto com a reaplicação.
    public void deslocar(Duration atraso) {
        if (status != StatusDose.PENDENTE) {
            throw new IllegalStateException("Só dose pendente é deslocada: dose " + id + " está " + status);
        }
        this.horarioPrevisto = horarioPrevisto.plus(atraso);
    }
}
