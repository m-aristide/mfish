package com.mfish.erp.ia;

import com.mfish.erp.produit.Produit;
import com.mfish.erp.quartier.Quartier;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Construit le prompt systeme a partir du catalogue, des quartiers desservis et des regles de ton.
 * Limite aussi le contexte aux N derniers messages.
 */
@Component
public class PromptBuilder {

    public static final int MAX_MESSAGES_CONTEXTE = 10;

    public String construire(final List<Produit> produits,
                             final List<Quartier> quartiers,
                             final List<LocalDate> joursLivraison) {
        final StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es l'assistant de commande d'un petit commercant qui livre ses clients. ")
                .append("Reponds en francais, de facon courtoise et breve.\n\n");

        prompt.append("Catalogue (nom et prix dans la plus petite unite monetaire) :\n");
        for (final Produit produit : produits) {
            prompt.append("- ").append(produit.getNom()).append(" : ").append(produit.getPrix()).append('\n');
        }

        prompt.append("\nQuartiers desservis :\n");
        for (final Quartier quartier : quartiers) {
            prompt.append("- ").append(quartier.getNom()).append('\n');
        }

        prompt.append("\nJours de livraison : ").append(joursLivraison).append("\n\n");

        prompt.append("Reponds UNIQUEMENT avec un objet JSON strict, sans texte autour, de cette forme :\n")
                .append("{\"reponse\":\"texte\",\"intention\":\"commande|question|modification|annulation|autre\",")
                .append("\"commande\":{\"produits\":[{\"nom\":\"...\",\"quantite\":1}],")
                .append("\"quartier\":\"...|null\",\"adresse\":\"...|null\",\"dateSouhaitee\":\"YYYY-MM-DD|null\",")
                .append("\"complet\":false},\"escalade\":false}\n")
                .append("Le prix est toujours lu en base, ne l'invente jamais. ")
                .append("Met escalade a true si tu ne peux pas traiter la demande ou si le client reclame.");
        return prompt.toString();
    }

    public List<String> limiterContexte(final List<String> messages) {
        if (messages.size() <= MAX_MESSAGES_CONTEXTE) {
            return messages;
        }
        return messages.subList(messages.size() - MAX_MESSAGES_CONTEXTE, messages.size());
    }
}
