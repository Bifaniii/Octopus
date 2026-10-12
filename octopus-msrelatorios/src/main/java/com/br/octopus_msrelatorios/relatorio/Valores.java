package com.br.octopus_msrelatorios.relatorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formatação dos valores no padrão brasileiro, comum ao PDF e aos textos dos filtros. */
public final class Valores {

    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Valores() {
    }

    public static String texto(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof LocalDateTime dataHora) {
            return DATA_HORA.format(dataHora);
        }
        if (valor instanceof LocalDate data) {
            return DATA.format(data);
        }
        return valor.toString();
    }
}
