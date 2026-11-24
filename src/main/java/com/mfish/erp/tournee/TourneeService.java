package com.mfish.erp.tournee;

import com.mfish.erp.client.Client;
import com.mfish.erp.commande.Commande;
import com.mfish.erp.commande.CommandeRepository;
import com.mfish.erp.commande.CommandeService;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.commun.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration applicative des tournees : generation/recalcul de la tournee du jour,
 * ordonnancement (RouteXL avec repli local), marquage des arrets.
 */
@Service
@Transactional(readOnly = true)
public class TourneeService {

    private static final double VITESSE_KMH = 25.0;
    private static final int DUREE_ARRET_MINUTES = 5;

    private final TourneeRepository tourneeRepository;
    private final ArretTourneeRepository arretRepository;
    private final CommandeRepository commandeRepository;
    private final RoutingPort routingPort;
    private final CommandeService commandeService;
    private final Clock clock;

    public TourneeService(final TourneeRepository tourneeRepository,
                          final ArretTourneeRepository arretRepository,
                          final CommandeRepository commandeRepository,
                          final RoutingPort routingPort,
                          final CommandeService commandeService,
                          final Clock clock) {
        this.tourneeRepository = tourneeRepository;
        this.arretRepository = arretRepository;
        this.commandeRepository = commandeRepository;
        this.routingPort = routingPort;
        this.commandeService = commandeService;
        this.clock = clock;
    }

    public Tournee trouver(final UUID id) {
        return tourneeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tournee introuvable : " + id));
    }

    public Tournee tourneeDuJour() {
        return tourneeParDate(LocalDate.now(clock));
    }

    public Tournee tourneeParDate(final LocalDate date) {
        return tourneeRepository.findByDate(date)
                .orElseGet(() -> tourneeRepository.save(new Tournee(date, StatutTournee.BROUILLON, 3.816110073189516, 11.489151853853572)));
    }

    public List<ArretTournee> arrets(final UUID tourneeId) {
        return arretRepository.findByTourneeIdOrderByPositionAsc(tourneeId);
    }

    /**
     * (Re)calcule la tournee du jour.
     */
    @Transactional
    @Scheduled(cron = "0 */10 * * * ?")
    public Tournee calculer() {
        return calculer(LocalDate.now(clock));
    }

    /**
     * (Re)calcule la tournee d'une date donnee : construit les arrets a optimiser a partir
     * des commandes a livrer, appelle RouteXL puis replie sur l'optimisation locale en cas
     * d'echec. Les commandes sans coordonnees exploitables (ni client ni quartier
     * geolocalise) ne peuvent pas etre optimisees : elles sont ajoutees a la fin de la
     * tournee, sans heure d'arrivee estimee.
     */
    @Transactional
    public Tournee calculer(final LocalDate date) {
        final Tournee tournee = tourneeParDate(date);
        final List<Commande> commandes = commandeRepository.findByDateLivraisonSouhaitee(tournee.getDate())
                .stream()
                .filter(commande -> commande.getStatut() == StatutCommande.A_PLANIFIER
                        || commande.getStatut() == StatutCommande.PLANIFIEE
                        || commande.getStatut() == StatutCommande.EN_COURS)
                .toList();

        arretRepository.deleteAllByTourneeId(tournee.getId());

        if (commandes.isEmpty()) {
            tournee.setStatut(StatutTournee.BROUILLON);
            return tourneeRepository.save(tournee);
        }

        final List<ArretAOptimiser> arrets = new ArrayList<>();
        final List<Commande> commandesSansCoordonnees = new ArrayList<>();
        for (final Commande commande : commandes) {
            final ArretAOptimiser arret = toArretAOptimiser(commande);
            if (arret == null) {
                commandesSansCoordonnees.add(commande);
            } else {
                arrets.add(arret);
            }
        }

        final List<ArretPlanifie> plan;
        if (arrets.isEmpty()) {
            plan = List.of();
        } else {
            final Coordonnees depart = depart(tournee, arrets);
            final LocalTime heureDepart = tournee.getHeureDepart() == null
                    ? LocalTime.of(8, 0)
                    : tournee.getHeureDepart();
            plan = optimiser(new TourneeRoutingInput(depart, heureDepart, arrets));
        }

        final Map<UUID, Commande> commandesParId = commandes.stream()
                .collect(Collectors.toMap(Commande::getId, Function.identity()));
        int position = 0;
        for (final ArretPlanifie planifie : plan.stream()
                .sorted(Comparator.comparingInt(ArretPlanifie::position)).toList()) {
            final Commande commande = commandesParId.get(planifie.commandeId());
            if (commande == null) {
                continue;
            }
            enregistrerArret(tournee, commande, position++, planifie.heureArrivee());
        }
        for (final Commande commande : commandesSansCoordonnees) {
            enregistrerArret(tournee, commande, position++, null);
        }

        tournee.setStatut(StatutTournee.VALIDEE);
        return tourneeRepository.save(tournee);
    }

    /**
     * Enregistre un arret de la tournee et passe la commande en {@code PLANIFIEE} si besoin.
     * L'heure estimee est {@code null} lorsque l'arret n'a pas pu etre optimise.
     */
    private void enregistrerArret(final Tournee tournee, final Commande commande, final int position,
                                  final LocalTime heureEstimee) {
        final ArretTournee arret = new ArretTournee(tournee, commande, position, StatutArret.A_LIVRER);
        arret.setHeureEstimee(heureEstimee);
        arretRepository.save(arret);
        if (commande.getStatut() == StatutCommande.A_PLANIFIER) {
            commandeService.changerStatut(commande, StatutCommande.PLANIFIEE);
        }
    }

    /**
     * Marque un arret (LIVRE/ABSENT/REPORTE) et, pour LIVRE, encaisse et livre la commande.
     */
    @Transactional
    public ArretTournee marquerArret(final UUID arretId, final StatutArret statut, final long montant) {
        final ArretTournee arret = arretRepository.findById(arretId)
                .orElseThrow(() -> new NotFoundException("Arret introuvable : " + arretId));
        arret.setStatut(statut);
        arret.setHeureReelle(LocalTime.now(clock));
        if (statut == StatutArret.LIVRE) {
            livrer(arret.getCommande(), montant);
        }
        return arretRepository.save(arret);
    }

    private void livrer(final Commande commande, final long montant) {
        if (commande.getStatut() == StatutCommande.PLANIFIEE) {
            commandeService.changerStatut(commande, StatutCommande.EN_COURS);
        }
        commandeService.changerStatut(commande, StatutCommande.LIVREE);
        final long restant = commande.getMontantTotal() - commande.getMontantPaye();
        final long aEncaisser = montant > 0 ? Math.min(montant, restant) : restant;
        if (aEncaisser > 0) {
            commandeService.enregistrerPaiement(commande, aEncaisser);
        }
    }

    private List<ArretPlanifie> optimiser(final TourneeRoutingInput input) {
        try {
            final List<ArretPlanifie> plan = routingPort.optimiser(input);
            if (plan != null && !plan.isEmpty()) {
                return plan;
            }
        } catch (RuntimeException exception) {
            // Repli sur l'optimisation locale (plan B) : voir DECISIONS.md.
        }
        return OptimisationLocale.optimiser(input, VITESSE_KMH, DUREE_ARRET_MINUTES);
    }

    private ArretAOptimiser toArretAOptimiser(final Commande commande) {
        final Coordonnees coordonnees = coordonnees(commande.getClient());
        if (coordonnees == null) {
            return null;
        }
        final int ordre = commande.getClient().getQuartier() == null
                ? 0
                : commande.getClient().getQuartier().getOrdreDefaut();
        return new ArretAOptimiser(commande.getId(), coordonnees, ordre);
    }

    private Coordonnees coordonnees(final Client client) {
        if (client.getLatitude() != null && client.getLongitude() != null) {
            return new Coordonnees(client.getLatitude(), client.getLongitude());
        }
        if (client.getQuartier() != null
                && client.getQuartier().getLatitude() != null
                && client.getQuartier().getLongitude() != null) {
            return new Coordonnees(client.getQuartier().getLatitude(),
                    client.getQuartier().getLongitude());
        }
        return null;
    }

    private Coordonnees depart(final Tournee tournee, final List<ArretAOptimiser> arrets) {
        if (tournee.getPointDepartLat() != null && tournee.getPointDepartLng() != null) {
            return new Coordonnees(tournee.getPointDepartLat(), tournee.getPointDepartLng());
        }
        return arrets.get(0).coordonnees();
    }
}


