package com.notivio.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy that runs repair() before migrate().
 * This clears any previously FAILED migration records from the schema history,
 * allowing the corrected migration scripts to re-run cleanly.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();   // clears FAILED entries from flyway_schema_history
            flyway.migrate();  // runs pending migrations normally
        };
    }
}
