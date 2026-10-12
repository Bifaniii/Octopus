package com.br.octopus_msusuario.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * No MySQL uma migration que falha no meio deixa uma linha "failed" no histórico, e o Flyway se recusa a subir até
 * alguém rodar repair. Aqui o repair roda sozinho, mas só nesse caso: ele apaga a linha com falha e a migration roda
 * de novo. Só é seguro porque as migrations que já falharam em produção (V7, em 12/10) foram reescritas para
 * continuar do estado parcial que deixaram.
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy migrarRetomandoFalhas() {
        return flyway -> {
            MigrationInfo[] falhas = Arrays.stream(flyway.info().all())
                    .filter(m -> m.getState() == MigrationState.FAILED)
                    .toArray(MigrationInfo[]::new);
            if (falhas.length > 0) {
                Arrays.stream(falhas).forEach(m ->
                        log.warn("Migration V{} ({}) falhou antes; o repair remove o registro e ela roda de novo",
                                m.getVersion(), m.getDescription()));
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
