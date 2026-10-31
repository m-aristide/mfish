package com.mfish.erp.quartier;

import java.util.UUID;

/**
 * Representation publique d'un quartier.
 */
public record QuartierDto(
        UUID id,
        String nom,
        String zone,
        Double latitude,
        Double longitude,
        int ordreDefaut,
        boolean actif) {

    public static QuartierDto from(final Quartier quartier) {
        return new QuartierDto(quartier.getId(), quartier.getNom(), quartier.getZone(),
                quartier.getLatitude(), quartier.getLongitude(), quartier.getOrdreDefaut(),
                quartier.isActif());
    }
}
