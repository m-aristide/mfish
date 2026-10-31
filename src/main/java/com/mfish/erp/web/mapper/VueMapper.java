package com.mfish.erp.web.mapper;

import com.mfish.erp.client.ClientDto;
import com.mfish.erp.commande.CommandeDto;
import com.mfish.erp.commande.LigneCommandeDto;
import com.mfish.erp.produit.ProduitDto;
import com.mfish.erp.quartier.QuartierDto;
import com.mfish.erp.tournee.ArretTournee;
import com.mfish.erp.web.dto.ArretVue;
import com.mfish.erp.web.dto.CommandeVue;
import com.mfish.erp.web.dto.LigneCommandeVue;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Assemble les vues presentees aux gabarits (noms et montants deja formats, cles i18n
 * pour les libelles). Aucune logique metier ici : uniquement de la presentation.
 */
@Component
public class VueMapper {

    private final MonnaieMapper monnaieMapper;

    public VueMapper(final MonnaieMapper monnaieMapper) {
        this.monnaieMapper = monnaieMapper;
    }

    public CommandeVue commandeVue(final CommandeDto commande,
                                   final Map<UUID, ClientDto> clients,
                                   final Map<UUID, QuartierDto> quartiers,
                                   final Map<UUID, ProduitDto> produits) {
        final ClientDto client = clients.get(commande.clientId());
        final UUID quartierId = client == null ? null : client.quartierId();
        final String quartierNom = quartierId != null && quartiers.containsKey(quartierId)
                ? quartiers.get(quartierId).nom() : null;
        final long restant = commande.montantTotal() - commande.montantPaye();
        final List<LigneCommandeVue> lignes = commande.lignes().stream()
                .map(ligne -> ligneVue(ligne, produits))
                .toList();
        return new CommandeVue(
                commande.id(),
                client == null ? null : client.nom(),
                client == null ? null : client.telephone(),
                quartierNom,
                "statut." + commande.statut().name(),
                commande.dateLivraisonSouhaitee(),
                "paiement." + commande.modePaiement().name(),
                "statut." + commande.statutPaiement().name(),
                commande.montantTotal(),
                monnaieMapper.formater(commande.montantTotal()),
                commande.montantPaye(),
                monnaieMapper.formater(commande.montantPaye()),
                restant,
                monnaieMapper.formater(restant),
                commande.notes(),
                "source." + commande.source().name(),
                lignes);
    }

    public ArretVue arretVue(final ArretTournee arret,
                             final Map<UUID, ClientDto> clients,
                             final Map<UUID, QuartierDto> quartiers) {
        final ClientDto client = clients.get(arret.getCommande().getClient().getId());
        final UUID quartierId = client == null ? null : client.quartierId();
        final String quartierNom = quartierId != null && quartiers.containsKey(quartierId)
                ? quartiers.get(quartierId).nom() : null;
        final long montantTotal = arret.getCommande().getMontantTotal();
        final long restant = montantTotal - arret.getCommande().getMontantPaye();
        return new ArretVue(
                arret.getId(),
                arret.getCommande().getId(),
                arret.getPosition(),
                arret.getHeureEstimee(),
                "statut." + arret.getStatut().name(),
                client == null ? null : client.nom(),
                client == null ? null : client.telephone(),
                quartierNom,
                client == null ? null : client.adresse(),
                montantTotal,
                monnaieMapper.formater(montantTotal),
                restant,
                monnaieMapper.formater(restant));
    }

    private LigneCommandeVue ligneVue(final LigneCommandeDto ligne,
                                      final Map<UUID, ProduitDto> produits) {
        final ProduitDto produit = produits.get(ligne.produitId());
        final long sousTotal = (long) ligne.quantite() * ligne.prixUnitaire();
        return new LigneCommandeVue(
                ligne.id(),
                produit == null ? "?" : produit.nom(),
                ligne.quantite(),
                ligne.prixUnitaire(),
                monnaieMapper.formater(ligne.prixUnitaire()),
                sousTotal,
                monnaieMapper.formater(sousTotal));
    }
}
