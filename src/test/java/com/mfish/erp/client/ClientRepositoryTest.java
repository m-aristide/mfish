package com.mfish.erp.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mfish.erp.TestcontainersConfiguration;
import com.mfish.erp.config.ApplicationConfig;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ApplicationConfig.class})
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private QuartierRepository quartierRepository;

    @Test
    @DisplayName("doit_rejeter_telephone_duplique")
    void doit_rejeter_telephone_duplique() {
        clientRepository.saveAndFlush(new Client("Awa", "+2250799999001"));

        assertThatThrownBy(() -> clientRepository.saveAndFlush(new Client("Bintou", "+2250799999001")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("doit_trouver_client_par_telephone")
    void doit_trouver_client_par_telephone() {
        clientRepository.saveAndFlush(new Client("Awa", "+2250799999002"));

        assertThat(clientRepository.findByTelephone("+2250799999002")).isPresent();
    }

    @Test
    @DisplayName("doit_attacher_client_a_un_quartier")
    void doit_attacher_client_a_un_quartier() {
        final Quartier quartier = quartierRepository.saveAndFlush(
                new Quartier("TestQuartier", "Nord", 5.35, -3.97, 1, true));
        final Client client = new Client("Awa", "+2250799999003");
        client.setQuartier(quartier);
        clientRepository.saveAndFlush(client);

        assertThat(clientRepository.findByQuartierId(quartier.getId())).hasSize(1);
    }
}
