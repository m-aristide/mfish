package com.mfish.erp.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.mfish.erp.commande.CommandeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableauDeBordServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Test
    @DisplayName("doit_utiliser_le_fuseau_horaire_configure_pour_le_jour")
    void doit_utiliser_le_fuseau_horaire_configure_pour_le_jour() {
        final Clock abidjan = Clock.fixed(Instant.parse("2026-11-01T23:30:00Z"),
                ZoneId.of("Africa/Abidjan"));

        new TableauDeBordService(commandeRepository, abidjan).livraisonsDuJour();

        verify(commandeRepository).findByDateLivraisonSouhaitee(LocalDate.of(2026, 11, 1));
    }

    @Test
    @DisplayName("doit_changer_de_jour_selon_le_fuseau_horaire")
    void doit_changer_de_jour_selon_le_fuseau_horaire() {
        final Clock paris = Clock.fixed(Instant.parse("2026-11-01T23:30:00Z"),
                ZoneId.of("Europe/Paris"));

        new TableauDeBordService(commandeRepository, paris).livraisonsDuJour();

        verify(commandeRepository).findByDateLivraisonSouhaitee(LocalDate.of(2026, 11, 2));
    }
}
