package com.br.octopus_msinternacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    // DATETIME não guarda fuso e o container roda em UTC: toda hora do sistema é a da clínica, em São Paulo.
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
