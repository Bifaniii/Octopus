package com.br.octopus_msprescricao.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;

public class ClockConfig {
    
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
