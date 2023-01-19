package ru.vukit.btm;

import android.location.Location;

import androidx.annotation.Keep;

@Keep
class MathModule {

    static final double notLatitude = 360.0;
    static final double notLongitude = 360.0;

    static Location createLocation(double latitude, double longitude, double bearing, double distance) {
        Location newLocation = new Location("newLocation");
        double radius = 6371000.0; // earth's mean radius in m
        double lat1 = Math.toRadians(latitude);
        double lng1 = Math.toRadians(longitude);
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / radius) + Math.cos(lat1) * Math.sin(distance / radius) * Math.cos(bearing));
        double lng2 = lng1 + Math.atan2(Math.sin(bearing) * Math.sin(distance / radius) * Math.cos(lat1), Math.cos(distance / radius) - Math.sin(lat1) * Math.sin(lat2));
        lng2 = (lng2 + Math.PI) % (2 * Math.PI) - Math.PI;
        // normalize to -180...+180
        if (lat2 == 0 || lng2 == 0) {
            newLocation.setLatitude(0.0);
            newLocation.setLongitude(0.0);
        } else {
            newLocation.setLatitude(Math.toDegrees(lat2));
            newLocation.setLongitude(Math.toDegrees(lng2));
        }
        return newLocation;
    }
}
