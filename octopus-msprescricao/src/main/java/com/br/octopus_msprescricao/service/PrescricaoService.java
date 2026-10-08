package com.br.octopus_msprescricao.service;

import com.br.octopus_msprescricao.domain.ItemPrescricao;
import com.br.octopus_msprescricao.domain.Prescricao;
import com.br.octopus_msprescricao.dto.request.PrescricaoRequest;
import com.br.octopus_msprescricao.dto.response.PrescricaoResponse;
import com.br.octopus_msprescricao.exception.RecursoNaoEncontradoException;
import com.br.octopus_msprescricao.repository.PrescricaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescricaoService {

    private final PrescricaoRepository prescricaoRepository;

    @Transactional
    public PrescricaoResponse criar(PrescricaoRequest request, String veterinarioEmail) {
        Prescricao prescricao = request.toEntity(veterinarioEmail);

        // TODO RN-03: bloquear itens cujos medicamentos têm interação proibida entre si. A lista fica no
        //  octopus-msmedications; falta decidir como consultar (chamada REST repassando o token, ou outra via).
        // TODO validar que a internação existe e está ATIVA, quando o octopus-msinternacao existir.

        prescricao.getItens().forEach(ItemPrescricao::gerarDoses);
        return PrescricaoResponse.from(prescricaoRepository.save(prescricao));
    }

    /** Devolve ativas e inativas, com o campo {@code ativo}. Com {@code internacaoId}, só as daquela internação. */
    @Transactional(readOnly = true)
    public List<PrescricaoResponse> listar(UUID internacaoId) {
        List<Prescricao> prescricoes = internacaoId == null
                ? prescricaoRepository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm"))
                : prescricaoRepository.findByInternacaoIdOrderByCriadoEmDesc(internacaoId);
        return prescricoes.stream().map(PrescricaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PrescricaoResponse buscar(UUID id) {
        return PrescricaoResponse.from(obter(id));
    }

    /** Nada é apagado: a prescrição fica com ativo = false. As doses dela não são alteradas. */
    @Transactional
    public PrescricaoResponse desativar(UUID id) {
        Prescricao prescricao = obter(id);
        prescricao.setAtivo(false);
        return PrescricaoResponse.from(prescricao);
    }

    private Prescricao obter(UUID id) {
        return prescricaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prescrição não encontrada com o id: " + id));
    }
}
