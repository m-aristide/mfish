package com.mfish.erp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base PostgreSQL partagee par les tests de persistance et d'integration.
 *
 * <p>Activee uniquement avec {@code app.tests.use-testcontainers=true} (Docker requis).
 * Par defaut, les tests utilisent H2 (voir {@code application-test.yaml}) car Docker n'est
 * pas toujours disponible dans l'environnement de developpement (voir {@code PROBLEMES.md}).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final String POSTGRES_IMAGE = "pgvector/pgvector:pg16";

    @Bean
    @ConditionalOnProperty(name = "app.tests.use-testcontainers", havingValue = "true")
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}

