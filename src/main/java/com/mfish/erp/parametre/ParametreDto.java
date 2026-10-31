package com.mfish.erp.parametre;

/**
 * Representation publique d'un parametre (cle/valeur).
 */
public record ParametreDto(String cle, String valeur) {

    public static ParametreDto from(final Parametre parametre) {
        return new ParametreDto(parametre.getCle(), parametre.getValeur());
    }
}
