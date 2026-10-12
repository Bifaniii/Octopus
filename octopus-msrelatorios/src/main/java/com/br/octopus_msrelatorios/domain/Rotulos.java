package com.br.octopus_msrelatorios.domain;

import java.util.Map;

/** Nomes legíveis dos enums que chegam dos outros módulos como texto. */
public final class Rotulos {

    private static final Map<String, String> PERFIS = Map.of(
            "ROLE_ADMIN", "Administrador",
            "ROLE_VETERINARIO", "Veterinário",
            "ROLE_AUXILIAR", "Auxiliar",
            "ROLE_RECEPCIONISTA", "Recepcionista");

    private static final Map<String, String> STATUS_INTERNACAO = Map.of(
            "ADMITIDA", "Admitida",
            "EM_TRATAMENTO", "Em tratamento",
            "ISOLAMENTO", "Isolamento",
            "ALTA_AUTORIZADA", "Alta autorizada",
            "ALTA_A_PEDIDO_DO_TUTOR", "Alta a pedido do tutor",
            "ENCERRADA", "Encerrada");

    private static final Map<String, String> TIPOS_BAIA = Map.of(
            "COLETIVA", "Coletiva",
            "ISOLAMENTO", "Isolamento",
            "NINHADA", "Ninhada");

    private Rotulos() {
    }

    public static String perfil(String role) {
        return PERFIS.getOrDefault(role, role);
    }

    public static String statusInternacao(String status) {
        return STATUS_INTERNACAO.getOrDefault(status, status);
    }

    public static String tipoBaia(String tipo) {
        return TIPOS_BAIA.getOrDefault(tipo, tipo);
    }
}
