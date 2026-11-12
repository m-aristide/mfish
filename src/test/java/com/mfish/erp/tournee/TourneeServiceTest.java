package com.mfish.erp.tournee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mfish.erp.client.Client;
import com.mfish.erp.commande.Commande;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.CommandeService;
import com.mfish.erp.commande.ModePaiement;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commande.StatutPaiement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TourneeServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 9);
    private static final UUID ID_OPTIMISABLE =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_SANS_COORDONNEES =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private TourneeRepository tourneeRepository;

    @Mock
    private ArretTourneeRepository arretRepository;

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private RoutingPort routingPort;

    @Mock
    private CommandeService commandeService;

    private TourneeService tourneeService;

    private Tournee tournee;

    @BeforeEach
    void setUp() {
        final Clock clock = Clock.fixed(Instant.parse("2026-11-09T06:00:00Z"), ZoneId.of("UTC"));
        tourneeService = new TourneeService(tourneeRepository, arretRepository, commandeRepository,
                routingPort, commandeService, clock);
        tournee = new Tournee(DATE, StatutTournee.BROUILLON, 3.8, 11.4);
        when(tourneeRepository.findByDate(DATE)).thenReturn(Optional.of(tournee));
        when(arretRepository.findByTourneeIdOrderByPositionAsc(any())).thenReturn(List.of());
        when(tourneeRepository.save(any(Tournee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("doit_ajouter_les_commandes_non_optimisables_a_la_fin_de_la_tournee")
    void doit_ajouter_les_commandes_non_optimisables_a_la_fin_de_la_tournee() {
        final Commande optimisable = commande(ID_OPTIMISABLE, clientAvecCoordonnees());
        final Commande sansCoordonnees = commande(ID_SANS_COORDONNEES,
                new Client("Sans adresse", "+237600000002"));
        when(commandeRepository.findByDateLivraisonSouhaitee(DATE))
                .thenReturn(List.of(sansCoordonnees, optimisable));
        when(routingPort.optimiser(any(TourneeRoutingInput.class)))
                .thenReturn(List.of(new ArretPlanifie(ID_OPTIMISABLE, 0, LocalTime.of(8, 15))));

        final Tournee resultat = tourneeService.calculer(DATE);

        final ArgumentCaptor<TourneeRoutingInput> inputCaptor =
                ArgumentCaptor.forClass(TourneeRoutingInput.class);
        verify(routingPort).optimiser(inputCaptor.capture());
        assertThat(inputCaptor.getValue().arrets())
                .extracting(ArretAOptimiser::commandeId)
                .containsExactly(ID_OPTIMISABLE);

        final List<ArretTournee> arrets = arretsEnregistres();
        assertThat(arrets).hasSize(2);
        assertThat(arrets.get(0).getCommande()).isSameAs(optimisable);
        assertThat(arrets.get(0).getPosition()).isZero();
        assertThat(arrets.get(0).getHeureEstimee()).isEqualTo(LocalTime.of(8, 15));
        assertThat(arrets.get(1).getCommande()).isSameAs(sansCoordonnees);
        assertThat(arrets.get(1).getPosition()).isEqualTo(1);
        assertThat(arrets.get(1).getHeureEstimee()).isNull();
        assertThat(arrets).allSatisfy(arret ->
                assertThat(arret.getStatut()).isEqualTo(StatutArret.A_LIVRER));
        assertThat(resultat.getStatut()).isEqualTo(StatutTournee.VALIDEE);
        verify(commandeService, times(2))
                .changerStatut(any(Commande.class), eq(StatutCommande.PLANIFIEE));
    }

    @Test
    @DisplayName("doit_ajouter_toutes_les_commandes_quand_aucune_n_est_optimisable")
    void doit_ajouter_toutes_les_commandes_quand_aucune_n_est_optimisable() {
        final Commande premiere = commande(ID_OPTIMISABLE,
                new Client("Sans adresse 1", "+237600000001"));
        final Commande seconde = commande(ID_SANS_COORDONNEES,
                new Client("Sans adresse 2", "+237600000002"));
        when(commandeRepository.findByDateLivraisonSouhaitee(DATE))
                .thenReturn(List.of(premiere, seconde));

        final Tournee resultat = tourneeService.calculer(DATE);

        verifyNoInteractions(routingPort);
        final List<ArretTournee> arrets = arretsEnregistres();
        assertThat(arrets).extracting(ArretTournee::getPosition).containsExactly(0, 1);
        assertThat(arrets).extracting(ArretTournee::getCommande).containsExactly(premiere, seconde);
        assertThat(arrets).allSatisfy(arret ->
                assertThat(arret.getHeureEstimee()).isNull());
        assertThat(resultat.getStatut()).isEqualTo(StatutTournee.VALIDEE);
    }

    private List<ArretTournee> arretsEnregistres() {
        final ArgumentCaptor<ArretTournee> captor = ArgumentCaptor.forClass(ArretTournee.class);
        verify(arretRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private Commande commande(final UUID id, final Client client) {
        final Commande commande = new Commande(client, StatutCommande.A_PLANIFIER,
                ModePaiement.ESPECES, StatutPaiement.NON_PAYE, SourceCommande.MANUEL);
        ReflectionTestUtils.setField(commande, "id", id);
        return commande;
    }

    private Client clientAvecCoordonnees() {
        final Client client = new Client("Avec coordonnees", "+237600000003");
        client.setLatitude(3.85);
        client.setLongitude(11.50);
        return client;
    }
}
