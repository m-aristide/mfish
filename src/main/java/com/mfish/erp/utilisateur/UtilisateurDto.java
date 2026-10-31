package com.mfish.erp.utilisateur;

import java.util.UUID;

/**
 * Representation publique d'un utilisateur (jamais le hash du mot de passe).
 */
public record UtilisateurDto(UUID id, String username, Role role, boolean actif) {

    public static UtilisateurDto from(final Utilisateur utilisateur) {
        return new UtilisateurDto(utilisateur.getId(), utilisateur.getUsername(),
                utilisateur.getRole(), utilisateur.isActif());
    }
}
