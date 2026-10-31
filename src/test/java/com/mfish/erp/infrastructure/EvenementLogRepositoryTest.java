package com.mfish.erp.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.mfish.erp.TestcontainersConfiguration;
import com.mfish.erp.config.ApplicationConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ApplicationConfig.class})
class EvenementLogRepositoryTest {

    @Autowired
    private EvenementLogRepository evenementLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("doit_persister_payload_jsonb")
    void doit_persister_payload_jsonb() {
        final EvenementLog log = evenementLogRepository.saveAndFlush(
                new EvenementLog("WEBHOOK", "{\"cle\":\"valeur\"}"));

        assertThat(log.getId()).isNotNull();
        assertThat(log.getPayload()).contains("cle");
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("doit_avoir_applique_les_migrations_v1_et_v2")
    void doit_avoir_applique_les_migrations_v1_et_v2() {
        final List<String> versions = jdbcTemplate.queryForList(
                "SELECT \"version\" FROM \"flyway_schema_history\" "
                        + "WHERE \"success\" = true ORDER BY \"installed_rank\"",
                String.class);

        assertThat(versions).contains("1", "2");
    }
}
