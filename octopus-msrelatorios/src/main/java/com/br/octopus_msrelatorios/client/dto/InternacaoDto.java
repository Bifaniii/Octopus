package com.br.octopus_msrelatorios.client.dto;

import java.time.LocalDateTime;

// O ms-internacao guarda nome e espécie do animal no momento da admissão; registradoPor é o e-mail.
public record InternacaoDto(Long id, Long animalId, String animalNome, String animalEspecie, Long baiaId,
                            String status, String motivo, LocalDateTime dataAdmissao, LocalDateTime dataAlta,
                            LocalDateTime dataSaida, String registradoPor) {

    public boolean ocupaBaia() {
        return !"ENCERRADA".equals(status);
    }
}
