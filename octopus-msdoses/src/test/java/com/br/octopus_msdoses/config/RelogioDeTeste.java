package com.br.octopus_msdoses.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Relógio que o teste acerta, para reproduzir "agora são 14h20" dos casos de teste do enunciado. */
public class RelogioDeTeste extends Clock {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private volatile Instant agora = Instant.now();

    public void acertar(LocalDateTime horario) {
        this.agora = horario.atZone(SAO_PAULO).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return SAO_PAULO;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        public RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }
}
