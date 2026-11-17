package com.mfish.erp.tournee;

import java.util.List;

/**
 * Port d'optimisation de tournee (implemente par l'adaptateur RouteXL).
 */
public interface RoutingPort {

    List<ArretPlanifie> optimiser(TourneeRoutingInput input);
}
