package com.mfish.erp.tournee;

/**
 * Distance de Haversine entre deux coordonnees (en kilometres).
 */
public final class DistanceHaversine {

    private static final double RAYON_TERRE_KM = 6371.0;

    private DistanceHaversine() {
    }

    public static double kilometres(final Coordonnees a, final Coordonnees b) {
        final double lat1 = Math.toRadians(a.latitude());
        final double lat2 = Math.toRadians(b.latitude());
        final double dLat = Math.toRadians(b.latitude() - a.latitude());
        final double dLng = Math.toRadians(b.longitude() - a.longitude());

        final double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RAYON_TERRE_KM * Math.asin(Math.sqrt(h));
    }
}
