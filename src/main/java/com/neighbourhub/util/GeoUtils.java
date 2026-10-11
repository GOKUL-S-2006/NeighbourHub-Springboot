package com.neighbourhub.util;

public final class GeoUtils {
    private GeoUtils() {}

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    /** Parses "lat,lon". Returns null if malformed (Node produced NaN, which never matched). */
    public static double[] parseLatLon(String location) {
        if (location == null) return null;
        String[] parts = location.split(",");
        if (parts.length < 2) return null;
        try {
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Haversine distance in meters. */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
