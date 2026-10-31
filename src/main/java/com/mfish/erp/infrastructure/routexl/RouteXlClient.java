package com.mfish.erp.infrastructure.routexl;

import com.mfish.erp.commun.AppelExterneException;
import com.mfish.erp.config.RetrySupport;
import com.mfish.erp.config.RoutexlProperties;
import com.mfish.erp.tournee.ArretAOptimiser;
import com.mfish.erp.tournee.ArretPlanifie;
import com.mfish.erp.tournee.RoutingPort;
import com.mfish.erp.tournee.TourneeRoutingInput;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur RouteXL (optimisation de tournee). Timeout, retry avec backoff, exceptions typees,
 * journalisation sans fuite de cle d'API.
 */
@Component
public class RouteXlClient implements RoutingPort {

    private static final Logger LOG = LoggerFactory.getLogger(RouteXlClient.class);

    private final RestClient restClient;
    private final RoutexlProperties properties;
    private final RetrySupport retrySupport;

    public RouteXlClient(final RestClient.Builder builder,
                         final RoutexlProperties properties,
                         final RetrySupport retrySupport) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(properties))
                .build();
        this.retrySupport = retrySupport;
    }

    @Override
    public List<ArretPlanifie> optimiser(final TourneeRoutingInput input) {
        LOG.info("Optimisation RouteXL de {} arrets", input.arrets().size());
        final RouteXlRequest request = RouteXlRequest.from(input);
        return retrySupport.executer(properties.retry(), () -> {
            final RouteXlResponse response = restClient.post()
                    .uri("/tour")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new AppelExterneException("RouteXL a repondu " + res.getStatusCode());
                    })
                    .body(RouteXlResponse.class);
            return response.toArrets();
        });
    }

    private static SimpleClientHttpRequestFactory requestFactory(final RoutexlProperties props) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        return factory;
    }

    private static String basicAuth(final RoutexlProperties props) {
        final String credentials = props.username() + ":" + props.password();
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Contrat d'entree de l'API RouteXL (simplifie) : point de depart + visites.
     */
    record RouteXlRequest(RouteXlPoint start, List<RouteXlVisit> visits) {

        static RouteXlRequest from(final TourneeRoutingInput input) {
            final List<RouteXlVisit> visites = input.arrets().stream()
                    .map(arret -> new RouteXlVisit(arret.commandeId().toString(),
                            new RouteXlPoint(arret.coordonnees().latitude(),
                                    arret.coordonnees().longitude())))
                    .toList();
            return new RouteXlRequest(
                    new RouteXlPoint(input.depart().latitude(), input.depart().longitude()),
                    visites);
        }
    }

    record RouteXlPoint(double lat, double lng) {
    }

    record RouteXlVisit(String id, RouteXlPoint location) {
    }

    /**
     * Contrat de sortie (simplifie) : visites ordonnees avec heure d'arrivee (HH:mm).
     */
    record RouteXlResponse(RouteXlResponseRoute route) {

        List<ArretPlanifie> toArrets() {
            if (route == null || route.visits() == null) {
                throw new AppelExterneException("Reponse RouteXL invalide : route absente");
            }
            final List<RouteXlResponseVisit> visites = route.visits();
            final List<ArretPlanifie> resultat = new java.util.ArrayList<>();
            for (int i = 0; i < visites.size(); i++) {
                final RouteXlResponseVisit visite = visites.get(i);
                resultat.add(new ArretPlanifie(
                        java.util.UUID.fromString(visite.id()),
                        i,
                        LocalTime.parse(visite.arrival())));
            }
            return resultat;
        }
    }

    record RouteXlResponseRoute(List<RouteXlResponseVisit> visits) {
    }

    record RouteXlResponseVisit(String id, String arrival) {
    }
}
