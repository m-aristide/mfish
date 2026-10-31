package com.mfish.erp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base PostgreSQL partagee par les tests de persistance et d'integration.
 * {@code @ServiceConnection} branche automatiquement la datasource sur le conteneur.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final String POSTGRES_IMAGE = "pgvector/pgvector:pg16";

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
