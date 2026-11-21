package com.mfish.erp.ia;

import com.mfish.erp.conversation.EtatCommande;
import com.mfish.erp.conversation.EtatCommandeJson;
import com.mfish.erp.produit.Produit;
import com.mfish.erp.quartier.Quartier;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Construit le prompt systeme a partir du catalogue, des quartiers desservis, des regles de ton
 * et de l'etat de la commande en cours. Limite aussi le contexte aux N derniers messages.
 */
@Component
public class PromptBuilder {

    public static final int MAX_MESSAGES_CONTEXTE = 10;

    public String construire(final List<Produit> produits,
                             final List<Quartier> quartiers,
                             final List<LocalDate> joursLivraison) {
        return construire(produits, quartiers, joursLivraison, EtatCommande.vide());
    }

    public String construire(final List<Produit> produits,
                             final List<Quartier> quartiers,
                             final List<LocalDate> joursLivraison,
                             final EtatCommande etat) {
        final EtatCommande etatConnu = etat == null ? EtatCommande.vide() : etat;
        final StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es l'assistant commercial WhatsApp de MFISH, un petit commercant de Yaounde ")
                .append("(Cameroun) qui vend du poisson silure frais. Reponds en francais, de facon ")
                .append("chaleureuse, courte et professionnelle.\n\n");

        prompt.append("Catalogue (nom et prix en FCFA) :\n");
        for (final Produit produit : produits) {
            prompt.append("- ").append(produit.getNom()).append(" : ").append(produit.getPrix()).append('\n');
        }
        if (produits.size() == 1) {
            prompt.append("Un seul produit est disponible : ").append(produits.get(0).getNom())
                    .append(". Ne demande JAMAIS quel produit le client veut : considere automatiquement ")
                    .append("qu'il s'agit de ").append(produits.get(0).getNom()).append(".\n");
        }

        prompt.append("\nFrais de livraison : 1 000 FCFA (tarif fixe), livraison uniquement a Yaounde.\n");

        prompt.append("\nQuartiers connus (a titre indicatif) :\n");
        for (final Quartier quartier : quartiers) {
            prompt.append("- ").append(quartier.getNom()).append('\n');
        }
        prompt.append("N'utilise PAS cette liste pour rejeter un quartier : accepte tout quartier ")
                .append("indique par le client a Yaounde, meme inconnu. Rejette uniquement une ville ")
                .append("clairement hors de Yaounde (exemple : Douala).\n");

        prompt.append("\nJours de livraison : ").append(joursLivraison).append("\n\n");

        prompt.append("Regles strictes :\n")
                .append("- N'invente JAMAIS la quantite : si le client n'a pas donne de quantite, ")
                .append("renvoie quantite 0. Ne suppose jamais 1 kg.\n")
                .append("- N'invente jamais le quartier, la date, l'heure, ni le nom du client.\n")
                .append("- Si le client dit merci, bonjour, au revoir, bonne journee ou un emoji de ")
                .append("politesse, reponds poliment et brievement, SANS repeter le recapitulatif.\n\n");

        prompt.append("Reponds UNIQUEMENT avec un objet JSON strict, sans texte autour, de cette forme :\n")
                .append("{\"reponse\":\"texte\",\"intention\":\"commande|question|modification|annulation|autre\",")
                .append("\"commande\":{\"produits\":[{\"nom\":\"...\",\"quantite\":1}],")
                .append("\"quartier\":\"...|null\",\"adresse\":\"...|null\",\"dateSouhaitee\":\"YYYY-MM-DD|null\",")
                .append("\"heureSouhaitee\":\"...|null\",\"complet\":false},\"escalade\":false}\n")
                .append("Le prix est toujours lu en base, ne l'invente jamais. ")
                .append("complet vaut true des que la quantite, le quartier et la date de livraison ")
                .append("sont connus. L'adresse precise est optionnelle : si le client donne ")
                .append("un quartier sans adresse, mets adresse a null et complet a true. ")
                .append("Met escalade a true si tu ne peux pas traiter la demande ou si le client reclame.");

        prompt.append("\n\nMESSAGE FINAL (quand le client confirme une commande complete) :\n")
                .append("\"Parfait {prenom si connu}, c'est transmis a notre equipe 🐟 Votre commande de ")
                .append("{quantite} kg de silure frais ({prix} FCFA/kg) pour {date} a {heure} au ")
                .append("{quartier} est notee. Livraison 1 000 FCFA, total {total} FCFA. L'equipe vous ")
                .append("confirme ici meme tres vite.\"\n")
                .append("Calcule total = quantite x prix + 1 000.\n\n");

        prompt.append("Etat de la commande en cours (JSON) : ")
                .append(EtatCommandeJson.versJson(etatConnu)).append('\n');
        prompt.append("Informations deja collectees : ")
                .append(etatConnu.resume().isBlank() ? "aucune" : etatConnu.resume()).append('\n');
        prompt.append("Informations encore manquantes : ")
                .append(etatConnu.champsManquants().isEmpty()
                        ? "aucune"
                        : String.join(", ", etatConnu.champsManquants()))
                .append('\n');
        prompt.append("Ne redemande JAMAIS une information deja presente dans cet etat. ")
                .append("Quand le client corrige une valeur, renvoie l'objet commande complet avec la ")
                .append("valeur corrigee. ")
                .append("Si le client ajoute une quantite, renvoie le total cumule. ")
                .append("Quand l'etat est complet et que le client repond par une confirmation ")
                .append("(oui, ok, c'est bon), garde la commande complete et reponds avec le message final.");
        return prompt.toString();
    }


    public List<String> limiterContexte(final List<String> messages) {
        if (messages.size() <= MAX_MESSAGES_CONTEXTE) {
            return messages;
        }
        return messages.subList(messages.size() - MAX_MESSAGES_CONTEXTE, messages.size());
    }
}
