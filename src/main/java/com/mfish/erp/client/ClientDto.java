package com.mfish.erp.client;

import java.util.UUID;

/**
 * Representation publique d'un client (les entites JPA ne sont jamais exposees).
 */
public record ClientDto(
        UUID id,
        String nom,
        String telephone,
        UUID quartierId,
        String adresse,
        String repere,
        Double latitude,
        Double longitude,
        String notes) {

    public static ClientDto from(final Client client) {
        return new ClientDto(
                client.getId(),
                client.getNom(),
                client.getTelephone(),
                client.getQuartier() == null ? null : client.getQuartier().getId(),
                client.getAdresse(),
                client.getRepere(),
                client.getLatitude(),
                client.getLongitude(),
                client.getNotes());
    }
}
