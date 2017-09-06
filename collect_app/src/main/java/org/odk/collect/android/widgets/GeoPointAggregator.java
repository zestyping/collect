package org.odk.collect.android.widgets;

import android.location.Location;
import android.location.LocationManager;

import org.javarosa.core.model.data.GeoPointData;

import java.util.ArrayList;
import java.util.List;

/**
 * Gathers Locations into a collection and computes the centroid of all the
 * collected locations that meet a specified accuracy threshold.
 */
public class GeoPointAggregator {
    private List<Location> locations = new ArrayList<>();

    public GeoPointAggregator() { }

    public int getNumPoints() {
        return locations.size();
    }

    public int getNumAcceptablePoints(double maxAccuracy) {
        int numPoints = 0;
        for (Location location : locations) {
            if (location.getAccuracy() <= maxAccuracy) {
                numPoints++;
            }
        }
        return numPoints;
    }

    public GeoPointData getCentroid(double accuracyThreshold) {
        // The average latitude and longitude aren't exactly the centroid
        // because the Earth is not flat, but it's not far off.
        double latSum = 0;
        double lonSum = 0;
        double altSum = 0;
        double maxAccuracy = 0;
        int numPoints = 0;
        for (Location location : locations) {
            if (location.getAccuracy() <= accuracyThreshold) {
                latSum += location.getLatitude();
                lonSum += location.getLongitude();
                altSum += location.getAltitude();
                maxAccuracy = Math.max(location.getAccuracy(), maxAccuracy);
                numPoints++;
            }
        }
        if (numPoints > 0) {
            return new GeoPointData(new double[] {
                latSum/numPoints,
                lonSum/numPoints,
                altSum/numPoints,
                maxAccuracy
            });
        }
        return null;
    }

    public void addLocation(Location location) {
        locations.add(location);
    }

    public double[] toDoubleArray() {
        double[] values = new double[locations.size() * 4];
        int i = 0;
        for (Location location : locations) {
            values[i++] = location.getLatitude();
            values[i++] = location.getLongitude();
            values[i++] = location.getAltitude();
            values[i++] = location.getAccuracy();
        }
        return values;
    }

    public static GeoPointAggregator fromDoubleArray(double[] values) {
        GeoPointAggregator points = new GeoPointAggregator();
        for (int i = 0; i < values.length; i += 4) {
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(values[i]);
            location.setLongitude(values[i + 1]);
            location.setAltitude(values[i + 2]);
            location.setAccuracy((float) values[i + 3]);
            points.addLocation(location);
        }
        return points;
    }
}
