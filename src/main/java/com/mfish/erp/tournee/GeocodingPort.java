package com.mfish.erp.tournee;

import java.util.Optional;

/**
 * Port de geocodage (adresse/quartier vers coordonnees GPS).
 */
public interface GeocodingPort {

    Optional<Coordonnees> geocoder(String adresse);
}
