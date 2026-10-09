package com.br.octopus_msinternacao.service;

import com.br.octopus_msinternacao.client.AnimalClient;
import com.br.octopus_msinternacao.client.BaiaClient;
import com.br.octopus_msinternacao.client.dto.AnimalDto;
import com.br.octopus_msinternacao.client.dto.BaiaDto;
import com.br.octopus_msinternacao.client.dto.TipoBaia;
import com.br.octopus_msinternacao.domain.Internacao;
import com.br.octopus_msinternacao.domain.enums.StatusInternacao;
import com.br.octopus_msinternacao.dto.request.AdmissaoRequest;
import com.br.octopus_msinternacao.dto.request.AltaAPedidoRequest;
import com.br.octopus_msinternacao.dto.request.EncerramentoRequest;
import com.br.octopus_msinternacao.dto.request.IsolamentoRequest;
import com.br.octopus_msinternacao.dto.response.InternacaoEventoResponse;
import com.br.octopus_msinternacao.dto.response.InternacaoResponse;
import com.br.octopus_msinternacao.exception.RecursoDuplicadoException;
import com.br.octopus_msinternacao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msinternacao.exception.RegraNegocioException;
import com.br.octopus_msinternacao.repository.InternacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.br.octopus_msinternacao.domain.enums.StatusInternacao.ENCERRADA;

@Service
@RequiredArgsConstructor
public class InternacaoService {

    private static final int MESES_VALIDADE_ANTIRRABICA = 12;

    private final InternacaoRepository internacaoRepository;
    private final AnimalClient animalClient;
    private final BaiaClient baiaClient;
    private final TransactionTemplate transacao;
    private final Clock clock;

    // As chamadas aos outros módulos vêm antes da transação, para nenhuma trava do banco ficar presa esperando a
    // rede. Dentro da transação fica só o que disputa vaga (RN-01) e a gravação.
    public InternacaoResponse admitir(AdmissaoRequest request, String usuario) {
        AnimalDto animal = animalClient.buscar(request.animalId());
        BaiaDto baia = buscarBaiaAtiva(request.baiaId());
        if (antirrabicaVencida(animal) && baia.tipo() != TipoBaia.ISOLAMENTO) {
            throw new RegraNegocioException("RN-02: antirrábica vencida ou sem registro; o animal só pode ser "
                    + "internado em baia de isolamento.");
        }

        return transacao.execute(status -> {
            if (internacaoRepository.existsByAnimalIdAndStatusNot(animal.id(), ENCERRADA)) {
                throw new RecursoDuplicadoException("O animal " + animal.nome() + " já tem uma internação aberta.");
            }
            exigirVaga(baia, animal.maeId());
            Internacao internacao = Internacao.admitir(animal.id(), animal.nome(), animal.especie(), animal.maeId(), baia.id(), request.motivo(),
                    usuario, agora());
            return InternacaoResponse.from(internacaoRepository.save(internacao));
        });
    }

    // Em tratamento → Isolamento (RN-02): só com vacinação irregular e para uma baia de isolamento com vaga.
    public InternacaoResponse isolar(Long id, IsolamentoRequest request, String usuario) {
        Internacao atual = buscarEntidade(id);
        AnimalDto animal = animalClient.buscar(atual.getAnimalId());
        if (!antirrabicaVencida(animal)) {
            throw new RegraNegocioException("RN-02: o isolamento só se aplica a animal com antirrábica vencida ou "
                    + "sem registro.");
        }
        BaiaDto baia = buscarBaiaAtiva(request.baiaId());
        if (baia.tipo() != TipoBaia.ISOLAMENTO) {
            throw new RegraNegocioException("A baia " + baia.nome() + " não é de isolamento.");
        }

        return transacao.execute(status -> {
            // Relida dentro da transação: é a versão desta leitura que o @Version confere no commit.
            Internacao internacao = buscarEntidade(id);
            // Animal admitido já no isolamento (RN-02 na entrada) só muda de status, sem ocupar outra vaga.
            if (!baia.id().equals(internacao.getBaiaId())) {
                exigirVaga(baia, internacao.getMaeId());
            }
            internacao.isolar(baia.id(), usuario, agora());
            return InternacaoResponse.from(internacao);
        });
    }

    // Admitida → Em tratamento. Chamada pelo msplantao a cada prescrição criada; idempotente.
    @Transactional
    public InternacaoResponse iniciarTratamento(Long id, String usuario) {
        Internacao internacao = buscarEntidade(id);
        internacao.iniciarTratamento(usuario, agora());
        return InternacaoResponse.from(internacao);
    }

    @Transactional
    public InternacaoResponse autorizarAlta(Long id, String usuario) {
        Internacao internacao = buscarEntidade(id);
        internacao.autorizarAlta(usuario, agora());
        return InternacaoResponse.from(internacao);
    }

    @Transactional
    public InternacaoResponse altaAPedidoDoTutor(Long id, AltaAPedidoRequest request, String usuario) {
        Internacao internacao = buscarEntidade(id);
        internacao.altaAPedidoDoTutor(request.termoResponsabilidade(), usuario, agora());
        return InternacaoResponse.from(internacao);
    }

    // RN-08: com a saída física registrada a internação é encerrada e a vaga da baia é liberada.
    @Transactional
    public InternacaoResponse encerrar(Long id, EncerramentoRequest request, String usuario) {
        Internacao internacao = buscarEntidade(id);
        internacao.encerrar(request.dataSaida(), usuario, agora());
        return InternacaoResponse.from(internacao);
    }

    @Transactional(readOnly = true)
    public List<InternacaoResponse> listar(StatusInternacao status, UUID baiaId, UUID animalId) {
        return internacaoRepository.listar(status, baiaId, animalId).stream().map(InternacaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InternacaoResponse buscar(Long id) {
        return InternacaoResponse.from(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<InternacaoEventoResponse> eventos(Long id) {
        return buscarEntidade(id).getEventos().stream().map(InternacaoEventoResponse::from).toList();
    }

    private Internacao buscarEntidade(Long id) {
        return internacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Internação não encontrada: " + id));
    }

    private BaiaDto buscarBaiaAtiva(UUID baiaId) {
        BaiaDto baia = baiaClient.buscar(baiaId);
        if (!baia.ativo()) {
            throw new RegraNegocioException("A baia " + baia.nome() + " está desativada.");
        }
        return baia;
    }

    // RN-01. A consulta trava as internações abertas da baia até o commit (ver InternacaoRepository).
    private void exigirVaga(BaiaDto baia, UUID maeId) {
        List<Internacao> ocupantes = internacaoRepository.findByBaiaIdAndStatusNot(baia.id(), ENCERRADA);
        if (ocupantes.size() >= baia.capacidadeEfetiva()) {
            throw new RegraNegocioException("RN-01: a baia " + baia.nome() + " está lotada ("
                    + ocupantes.size() + "/" + baia.capacidadeEfetiva() + ").");
        }
        if (baia.tipo() == TipoBaia.NINHADA) {
            if (maeId == null) {
                throw new RegraNegocioException("RN-01: baia de ninhada só recebe filhote com a mãe cadastrada.");
            }
            if (ocupantes.stream().anyMatch(o -> !maeId.equals(o.getMaeId()))) {
                throw new RegraNegocioException("RN-01: a baia " + baia.nome() + " já abriga filhotes de outra mãe.");
            }
        }
    }

    // Vencida = sem registro ou aplicada há mais de 12 meses. Com exatamente 12 meses ainda vale.
    private boolean antirrabicaVencida(AnimalDto animal) {
        LocalDate data = animal.dataUltimaAntirrabica();
        return data == null || data.plusMonths(MESES_VALIDADE_ANTIRRABICA).isBefore(LocalDate.now(clock));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
