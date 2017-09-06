package org.odk.collect.android.widgets;

import android.location.Location;

import org.osmdroid.util.GeoPoint;

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

    public GeoPoint getCentroid(double maxAccuracy) {
        // The Earth is round, so the average latitude and average longitude
        // aren't exactly the centroid, but it's not far off.
        double latSum = 0;
        double lonSum = 0;
        double altSum = 0;
        int numPoints = 0;
        for (Location location : locations) {
            if (location.getAccuracy() <= maxAccuracy) {
                latSum += location.getLatitude();
                lonSum += location.getLongitude();
                altSum += location.getAltitude();
                numPoints++;
            }
        }
        if (numPoints > 0) {
            return new GeoPoint(latSum/numPoints, lonSum/numPoints, altSum/numPoints);
        }
        return null;
    }

    public void addLocation(Location location) {
        locations.add(location);
    }
}
