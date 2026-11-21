package com.br.octopus_msdoses.service;

import com.br.octopus_msdoses.client.CadastrosClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Aplicar a vacina antirrábica atualiza a data da última antirrábica no cadastro do animal (msusuario), que é o que
 * a RN-02 consulta na internação. A vacina é cadastrada, prescrita e agendada como qualquer medicamento; ela é
 * reconhecida pelo nome: qualquer medicamento com "antirrábica" no nome comercial, sem diferença de acento,
 * maiúscula ou hífen ("Antirrábica", "VACINA ANTIRRABICA", "anti-rábica").
 *
 * O resultado da conferência fica guardado por medicamento enquanto o serviço roda: só a primeira aplicação de cada
 * medicamento pergunta o nome ao msmedications. Um medicamento renomeado para incluir "antirrábica" depois de já ter
 * sido aplicado só passa a contar quando o serviço reiniciar.
 */
@Component
@RequiredArgsConstructor
public class Antirrabica {

    private static final Pattern ANTIRRABICA = Pattern.compile("antir{1,2}abica");

    private final CadastrosClient cadastros;
    private final Map<Long, Boolean> ehVacina = new ConcurrentHashMap<>();

    /**
     * Se o medicamento for a antirrábica, grava a data no cadastro do animal da internação. Qualquer falha sobe
     * (503 ou 404), e quem chamou não registra a aplicação: como ela não pode ser editada depois (RN-05), é a única
     * forma de a data da vacina nunca ficar desatualizada.
     */
    public void registrarSeFor(Long medicacaoId, Long internacaoId, LocalDate dataDaAplicacao) {
        boolean vacina = ehVacina.computeIfAbsent(medicacaoId, id -> ehAntirrabica(cadastros.nomeDoMedicamento(id)));
        if (!vacina) {
            return;
        }
        cadastros.registrarAntirrabica(cadastros.animalDaInternacao(internacaoId), dataDaAplicacao);
    }

    static boolean ehAntirrabica(String nome) {
        if (nome == null) {
            return false;
        }
        String normalizado = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        // "antirrábica" junto tem dois erres; "anti-rábica", com hífen, tem um.
        return ANTIRRABICA.matcher(normalizado).find();
    }
}
