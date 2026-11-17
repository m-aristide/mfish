package com.mfish.erp.commande;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Matrice complete des transitions de statut (5 x 5).
 */
class StatutCommandeTest {

    @Test
    @DisplayName("doit_autoriser_exactement_les_transitions_definies")
    void doit_autoriser_exactement_les_transitions_definies() {
        final Set<Map.Entry<StatutCommande, StatutCommande>> autorisees = Set.of(
                Map.entry(StatutCommande.A_PLANIFIER, StatutCommande.PLANIFIEE),
                Map.entry(StatutCommande.A_PLANIFIER, StatutCommande.ANNULEE),
                Map.entry(StatutCommande.PLANIFIEE, StatutCommande.EN_COURS),
                Map.entry(StatutCommande.PLANIFIEE, StatutCommande.A_PLANIFIER),
                Map.entry(StatutCommande.PLANIFIEE, StatutCommande.ANNULEE),
                Map.entry(StatutCommande.EN_COURS, StatutCommande.LIVREE),
                Map.entry(StatutCommande.EN_COURS, StatutCommande.PLANIFIEE)
        );

        for (final StatutCommande de : StatutCommande.values()) {
            for (final StatutCommande vers : StatutCommande.values()) {
                assertThat(de.peutPasserA(vers))
                        .as("transition %s -> %s", de, vers)
                        .isEqualTo(autorisees.contains(Map.entry(de, vers)));
            }
        }
    }
}
