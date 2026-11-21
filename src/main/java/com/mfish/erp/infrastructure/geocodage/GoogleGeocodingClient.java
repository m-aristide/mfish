package com.mfish.erp.infrastructure.geocodage;

import com.mfish.erp.commun.AppelExterneException;
import com.mfish.erp.config.GeocodingProperties;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.tournee.Coordonnees;
import com.mfish.erp.tournee.GeocodingPort;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur Google Geocoding (API Geocoding). Timeout, retry avec backoff, exceptions
 * typees, journalisation sans fuite de cle d'API.
 */
@Component
public class GoogleGeocodingClient implements GeocodingPort {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleGeocodingClient.class);

    private static final String STATUT_OK = "OK";
    private static final String STATUT_AUCUN_RESULTAT = "ZERO_RESULTS";

    private final RestClient restClient;
    private final GeocodingProperties properties;
    private final RetrySupport retrySupport;

    public GoogleGeocodingClient(final RestClient.Builder builder,
                                 final GeocodingProperties properties,
                                 final RetrySupport retrySupport) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build();
        this.retrySupport = retrySupport;
    }

    @Override
    public Optional<Coordonnees> geocoder(final String adresse) {
        LOG.info("Geocodage Google d'un quartier");
        final GoogleGeocodingResponse response = retrySupport.executer(properties.retry(),
                () -> restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/maps/api/geocode/json")
                                .queryParam("address", adresse)
                                .queryParam("region", "cm")
                                .queryParam("key", properties.apiKey())
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            throw new AppelExterneException(
                                    "Google Geocoding a repondu " + res.getStatusCode());
                        })
                        .body(GoogleGeocodingResponse.class));

        if (response == null || response.status() == null) {
            throw new AppelExterneException("Reponse Google Geocoding invalide");
        }
        if (STATUT_AUCUN_RESULTAT.equals(response.status())) {
            return Optional.empty();
        }
        if (!STATUT_OK.equals(response.status())) {
            throw new AppelExterneException("Google Geocoding a repondu " + response.status());
        }
        if (response.results() == null || response.results().isEmpty()) {
            return Optional.empty();
        }
        final GoogleGeocodingResult premier = response.results().get(0);
        if (premier.geometry() == null || premier.geometry().location() == null) {
            throw new AppelExterneException(
                    "Reponse Google Geocoding invalide : localisation absente");
        }
        final GoogleGeocodingLocation location = premier.geometry().location();
        return Optional.of(new Coordonnees(location.lat(), location.lng()));
    }

    private static SimpleClientHttpRequestFactory requestFactory(final GeocodingProperties props) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        return factory;
    }

    /** Reponse de l'API Geocoding (seuls les champs utiles sont mappes). */
    record GoogleGeocodingResponse(String status, List<GoogleGeocodingResult> results) {
    }

    record GoogleGeocodingResult(GoogleGeocodingGeometry geometry) {
    }

    record GoogleGeocodingGeometry(GoogleGeocodingLocation location) {
    }

    record GoogleGeocodingLocation(double lat, double lng) {
    }
}
