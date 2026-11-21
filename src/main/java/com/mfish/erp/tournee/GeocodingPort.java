package com.mfish.erp.tournee;

import java.util.Optional;

/**
 * Port de geocodage (implemente par l'adaptateur Google Geocoding).
 * Traduit une adresse ou un quartier en coordonnees GPS.
 */
public interface GeocodingPort {

    /**
     * Geocode une adresse complete (ex. {@code Bastos, Yaounde, Cameroun}).
     *
     * @param adresse adresse a geocoder (non nulle)
     * @return coordonnees GPS trouvees, ou vide si aucun resultat
     */
    Optional<Coordonnees> geocoder(String adresse);
}
