package com.mfish.erp.commande;

import com.mfish.erp.client.Client;
import com.mfish.erp.client.ClientRepository;
import com.mfish.erp.commun.NotFoundException;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.produit.ProduitRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration applicative des commandes : resolution des references (client, produits),
 * lecture des prix en base, machine a etats et encaissement.
 */
@Service
@Transactional(readOnly = true)
public class CommandeApplicationService {

    private final CommandeService commandeService;
    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;

    public CommandeApplicationService(final CommandeService commandeService,
                                      final CommandeRepository commandeRepository,
                                      final ClientRepository clientRepository,
                                      final ProduitRepository produitRepository) {
        this.commandeService = commandeService;
        this.commandeRepository = commandeRepository;
        this.clientRepository = clientRepository;
        this.produitRepository = produitRepository;
    }

    public List<CommandeDto> lister() {
        return commandeRepository.findAll().stream().map(CommandeDto::from).toList();
    }

    public List<CommandeDto> listerParClient(final UUID clientId) {
        return commandeRepository.findByClientId(clientId).stream().map(CommandeDto::from).toList();
    }

    public Page<CommandeDto> rechercher(final StatutCommande statut, final LocalDate date,
                                        final UUID quartierId, final String recherche,
                                        final Pageable pageable) {
        final String motif = recherche == null || recherche.isBlank()
                ? null
                : "%" + recherche.toLowerCase() + "%";
        final Page<Commande> page = commandeRepository.rechercher(statut, date, quartierId,
                motif, pageable);
        return new PageImpl<>(page.getContent().stream().map(CommandeDto::from).toList(),
                pageable, page.getTotalElements());
    }

    public CommandeDto trouver(final UUID id) {
        return commandeRepository.findById(id).map(CommandeDto::from)
                .orElseThrow(() -> new NotFoundException("Commande introuvable : " + id));
    }

    @Transactional
    public CommandeDto creer(final CommandeRequest request) {
        final Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new NotFoundException("Client introuvable : " + request.clientId()));

        final List<LigneCommande> lignes = new ArrayList<>();
        for (final LigneCommandeRequest ligneRequest : request.lignes()) {
            final Produit produit = produitRepository.findById(ligneRequest.produitId())
                    .orElseThrow(() -> new NotFoundException("Produit introuvable : " + ligneRequest.produitId()));
            lignes.add(new LigneCommande(produit, ligneRequest.quantite(), produit.getPrix()));
        }

        final Commande commande = commandeService.creer(client, lignes, request.modePaiement(),
                request.source());
        commande.setDateLivraisonSouhaitee(request.dateLivraisonSouhaitee());
        commande.setNotes(request.notes());
        return CommandeDto.from(commande);
    }

    @Transactional
    public CommandeDto changerStatut(final UUID id, final StatutCommande cible) {
        final Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Commande introuvable : " + id));
        return CommandeDto.from(commandeService.changerStatut(commande, cible));
    }

    @Transactional
    public CommandeDto enregistrerPaiement(final UUID id, final long montant) {
        final Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Commande introuvable : " + id));
        return CommandeDto.from(commandeService.enregistrerPaiement(commande, montant));
    }

    @Transactional
    public CommandeDto annuler(final UUID id) {
        final Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Commande introuvable : " + id));
        return CommandeDto.from(commandeService.annuler(commande));
    }
}
