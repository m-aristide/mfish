package com.mfish.erp.ia;

/**
 * Resultat d'un traitement IA : reponse au client, besoin d'escalade, commande creee.
 */
public record ResultatIa(String reponseAuClient, boolean escalade, boolean commandeCreee) {
}
