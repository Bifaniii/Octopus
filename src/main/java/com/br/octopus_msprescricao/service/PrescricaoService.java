package com.br.octopus_msprescricao.service;

import com.br.octopus_msprescricao.client.InternacaoClient;
import com.br.octopus_msprescricao.client.MedicacaoClient;
import com.br.octopus_msprescricao.client.dto.InternacaoDto;
import com.br.octopus_msprescricao.client.dto.MedicacaoDto;
import com.br.octopus_msprescricao.domain.Prescricao;
import com.br.octopus_msprescricao.dto.request.ItemPrescricaoRequest;
import com.br.octopus_msprescricao.dto.request.PrescricaoRequest;
import com.br.octopus_msprescricao.dto.response.PrescricaoResponse;
import com.br.octopus_msprescricao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msprescricao.exception.RegraDeNegocioException;
import com.br.octopus_msprescricao.repository.PrescricaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PrescricaoService {

    // Depois da alta (autorizada ou a pedido do tutor) e com a internação encerrada não se prescreve mais.
    private static final Set<String> STATUS_QUE_ACEITAM_PRESCRICAO = Set.of("ADMITIDA", "EM_TRATAMENTO", "ISOLAMENTO");

    private final PrescricaoRepository prescricaoRepository;
    private final InternacaoClient internacaoClient;
    private final MedicacaoClient medicacaoClient;
    private final Clock clock;

    /**
     * Sem @Transactional no método: as chamadas ao ms-internacao vêm antes e não podem segurar transação esperando
     * a rede. O iniciar-tratamento vem antes de gravar porque é idempotente: se ele falhar, nada é gravado; se a
     * gravação falhar depois, a internação fica em tratamento e a nova tentativa do veterinário não muda nada lá.
     */
    public PrescricaoResponse criar(PrescricaoRequest request, String veterinarioEmail) {
        InternacaoDto internacao = internacaoClient.buscar(request.internacaoId());
        if (!STATUS_QUE_ACEITAM_PRESCRICAO.contains(internacao.status())) {
            throw new RegraDeNegocioException("A internação " + internacao.id() + " está " + internacao.status()
                    + " e não aceita nova prescrição.");
        }

        exigirSemInteracaoProibida(request, internacao.id());

        internacaoClient.iniciarTratamento(internacao.id());

        Prescricao prescricao = request.toEntity(veterinarioEmail, LocalDateTime.now(clock));
        return PrescricaoResponse.from(prescricaoRepository.save(prescricao));
    }

    /** Devolve ativas e inativas, com o campo {@code ativo}. Com {@code internacaoId}, só as daquela internação. */
    @Transactional(readOnly = true)
    public List<PrescricaoResponse> listar(Long internacaoId) {
        List<Prescricao> prescricoes = internacaoId == null
                ? prescricaoRepository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm"))
                : prescricaoRepository.findByInternacaoIdOrderByCriadoEmDesc(internacaoId);
        return prescricoes.stream().map(PrescricaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PrescricaoResponse buscar(Long id) {
        return PrescricaoResponse.from(obter(id));
    }

    /** Nada é apagado: a prescrição fica com ativo = false. */
    @Transactional
    public PrescricaoResponse desativar(Long id) {
        Prescricao prescricao = obter(id);
        prescricao.setAtivo(false);
        return PrescricaoResponse.from(prescricao);
    }

    /**
     * RN-03: bloqueia a prescrição se dois medicamentos dela têm interação proibida, ou se um deles tem interação
     * proibida com algo que o animal já recebe (prescrições ativas da mesma internação): todas as prescrições
     * ativas valem para o mesmo animal. A lista de interações vem do octopus-msmedications.
     */
    private void exigirSemInteracaoProibida(PrescricaoRequest request, Long internacaoId) {
        Map<Long, MedicacaoDto> novos = new LinkedHashMap<>();
        for (ItemPrescricaoRequest item : request.itens()) {
            novos.computeIfAbsent(item.medicacaoId(), medicacaoClient::buscar);
        }

        List<MedicacaoDto> lista = new ArrayList<>(novos.values());
        for (int i = 0; i < lista.size(); i++) {
            for (int j = i + 1; j < lista.size(); j++) {
                MedicacaoDto a = lista.get(i);
                MedicacaoDto b = lista.get(j);
                if (a.nomeDaInteracaoCom(b.id()) != null || b.nomeDaInteracaoCom(a.id()) != null) {
                    throw new RegraDeNegocioException("RN-03: " + a.nomeComercial() + " e " + b.nomeComercial()
                            + " têm interação proibida e não podem ser prescritos juntos.");
                }
            }
        }

        // Basta a lista do medicamento novo: o msmedications grava o par nos dois sentidos.
        for (Long emUso : prescricaoRepository.medicacoesEmUso(internacaoId)) {
            for (MedicacaoDto novo : lista) {
                String nomeEmUso = novo.nomeDaInteracaoCom(emUso);
                if (nomeEmUso != null) {
                    throw new RegraDeNegocioException("RN-03: " + novo.nomeComercial() + " tem interação proibida com "
                            + nomeEmUso + ", já prescrito nesta internação.");
                }
            }
        }
    }

    private Prescricao obter(Long id) {
        return prescricaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prescrição não encontrada com o id: " + id));
    }
}
