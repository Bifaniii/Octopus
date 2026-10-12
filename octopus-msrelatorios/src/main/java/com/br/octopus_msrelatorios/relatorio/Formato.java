package com.br.octopus_msrelatorios.relatorio;

public enum Formato {

    PDF("application/pdf", "pdf"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String mediaType;
    private final String extensao;

    Formato(String mediaType, String extensao) {
        this.mediaType = mediaType;
        this.extensao = extensao;
    }

    public String mediaType() {
        return mediaType;
    }

    public String extensao() {
        return extensao;
    }
}
