package com.mfish.erp.infrastructure.geocodage;

import com.mfish.erp.config.GeocodingProperties;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.tournee.Coordonnees;
import com.mfish.erp.tournee.GeocodingPort;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur de geocodage Nominatim (OpenStreetMap).
 */
@Component
public class NominatimGeocodingClient implements GeocodingPort {

    private static final Logger LOG = LoggerFactory.getLogger(NominatimGeocodingClient.class);

    private final RestClient restClient;
    private final GeocodingProperties properties;
    private final RetrySupport retrySupport;

    public NominatimGeocodingClient(final RestClient.Builder builder,
                                    final GeocodingProperties properties,
                                    final RetrySupport retrySupport) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.USER_AGENT, "mfish/1.0")
                .build();
        this.retrySupport = retrySupport;
    }

    @Override
    public Optional<Coordonnees> geocoder(final String adresse) {
        LOG.info("Geocodage de l'adresse (taille {} caracteres)", adresse == null ? 0 : adresse.length());
        return retrySupport.executer(properties.retry(), () -> {
            final List<NominatimResult> resultats = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", adresse)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NominatimResult>>() { });
            if (resultats == null || resultats.isEmpty()) {
                return Optional.empty();
            }
            final NominatimResult resultat = resultats.get(0);
            return Optional.of(new Coordonnees(
                    Double.parseDouble(resultat.lat()), Double.parseDouble(resultat.lon())));
        });
    }

    private static SimpleClientHttpRequestFactory requestFactory(final GeocodingProperties props) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        return factory;
    }

    record NominatimResult(String lat, String lon) {
    }
}
