package com.br.octopus_msinternacao.domain;

import com.br.octopus_msinternacao.domain.enums.StatusInternacao;
import com.br.octopus_msinternacao.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.br.octopus_msinternacao.domain.enums.StatusInternacao.*;

// Sem @Setter/@Builder de propósito: o status só muda pelos métodos de negócio, que conferem a transição e
// registram o evento. O construtor protegido existe só para o JPA.
@Entity
@Table(name = "tb_internacoes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Internacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "animal_id", nullable = false)
    private UUID animalId;

    // Cópia do nome e da espécie na admissão: o painel mostra quem está internado sem consultar o msusuario.
    @Column(name = "animal_nome", length = 100, nullable = false)
    private String animalNome;

    @Column(name = "animal_especie", length = 250, nullable = false)
    private String animalEspecie;

    @Column(name = "baia_id", nullable = false)
    private UUID baiaId;

    // Cópia da mãe do animal na admissão (RN-01 da ninhada). Nula quando a mãe não é conhecida.
    @Column(name = "mae_id")
    private UUID maeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private StatusInternacao status;

    @Column(name = "motivo", length = 500, nullable = false)
    private String motivo;

    @Column(name = "termo_responsabilidade", length = 500)
    private String termoResponsabilidade;

    @Column(name = "data_admissao", nullable = false)
    private LocalDateTime dataAdmissao;

    @Column(name = "data_alta")
    private LocalDateTime dataAlta;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;

    @Column(name = "registrado_por", nullable = false)
    private String registradoPor;

    // Duas pessoas mexendo na mesma internação ao mesmo tempo: a segunda a salvar recebe erro em vez de
    // sobrescrever a primeira.
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @OneToMany(mappedBy = "internacao", cascade = CascadeType.ALL)
    @OrderBy("dataHora")
    private List<InternacaoEvento> eventos = new ArrayList<>();

    // As validações que dependem de outros módulos (RN-01, RN-02) ficam no service; aqui só o que a
    // própria internação sabe responder.
    public static Internacao admitir(UUID animalId, String animalNome, String animalEspecie, UUID maeId,
                                     UUID baiaId, String motivo, String usuario,
                                     LocalDateTime agora) {
        Internacao internacao = new Internacao();
        internacao.animalId = animalId;
        internacao.animalNome = animalNome;
        internacao.animalEspecie = animalEspecie;
        internacao.maeId = maeId;
        internacao.baiaId = baiaId;
        internacao.motivo = motivo;
        internacao.status = ADMITIDA;
        internacao.dataAdmissao = agora;
        internacao.registradoPor = usuario;
        internacao.registrarEvento(null, usuario, agora);
        return internacao;
    }

    // Admitida → Em tratamento. Quem garante a prescrição ativa é o msplantao, que chama esta transição a cada
    // prescrição criada; por isso é idempotente: se o tratamento já começou, não faz nada.
    public void iniciarTratamento(String usuario, LocalDateTime agora) {
        if (status == EM_TRATAMENTO || status == ISOLAMENTO) {
            return;
        }
        exigirTransicao(EM_TRATAMENTO);
        aplicar(EM_TRATAMENTO, usuario, agora);
    }

    // RN-02: o service já conferiu que a vacina está irregular e que a baia nova é de isolamento com vaga.
    public void isolar(UUID baiaIsolamentoId, String usuario, LocalDateTime agora) {
        exigirTransicao(ISOLAMENTO);
        this.baiaId = baiaIsolamentoId;
        aplicar(ISOLAMENTO, usuario, agora);
    }

    public void autorizarAlta(String usuario, LocalDateTime agora) {
        exigirTransicao(ALTA_AUTORIZADA);
        this.dataAlta = agora;
        aplicar(ALTA_AUTORIZADA, usuario, agora);
    }

    public void altaAPedidoDoTutor(String termo, String usuario, LocalDateTime agora) {
        exigirTransicao(ALTA_A_PEDIDO_DO_TUTOR);
        if (termo == null || termo.isBlank()) {
            throw new RegraNegocioException("Alta a pedido do tutor exige o termo de responsabilidade registrado.");
        }
        this.termoResponsabilidade = termo;
        this.dataAlta = agora;
        aplicar(ALTA_A_PEDIDO_DO_TUTOR, usuario, agora);
    }

    // RN-08: a internação só é encerrada (e a baia liberada) com o registro da saída física.
    public void encerrar(LocalDateTime saida, String usuario, LocalDateTime agora) {
        exigirTransicao(ENCERRADA);
        if (saida == null) {
            throw new RegraNegocioException("RN-08: a internação só pode ser encerrada com o registro da saída física.");
        }
        if (saida.isBefore(dataAlta) || saida.isAfter(agora)) {
            throw new RegraNegocioException("A saída física deve estar entre a alta e o momento atual.");
        }
        this.dataSaida = saida;
        aplicar(ENCERRADA, usuario, agora);
    }

    private void exigirTransicao(StatusInternacao destino) {
        if (!status.podeIrPara(destino)) {
            throw new RegraNegocioException("Transição não permitida: " + status + " → " + destino + ".");
        }
    }

    private void aplicar(StatusInternacao destino, String usuario, LocalDateTime agora) {
        StatusInternacao anterior = this.status;
        this.status = destino;
        registrarEvento(anterior, usuario, agora);
    }

    private void registrarEvento(StatusInternacao anterior, String usuario, LocalDateTime agora) {
        eventos.add(InternacaoEvento.builder()
                .internacao(this)
                .statusAnterior(anterior)
                .statusNovo(status)
                .baiaId(baiaId)
                .usuario(usuario)
                .dataHora(agora)
                .build());
    }
}
