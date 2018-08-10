package org.odk.collect.android.map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.util.List;

/**
 * Interface for Fragments that render a map view.  The plan is to have one
 * implementation for each map SDK, e.g. GoogleMapFragment, OsmMapFragment, etc.
 */
public interface MapFragment {
    /**
     * Adds the map Fragment to an activity.  The containerId should be the
     * resource ID of a View, into which the map view will be placed.  The
     * listener will be invoked with this MapFragment when the map is ready
     * to be used, or with null if there is a problem initializing the map.
     */
    void addTo(@NonNull FragmentActivity activity, int containerId, @Nullable ReadyListener listener);

    /** Gets the View that is displaying the map. */
    View getView();

    /**
     * Gets the current zoom level.  For maps that only support zooming by
     * powers of 2, the zoom level will always be an integer.
     */
    double getZoom();

    /**
     * Changes the zoom level to bring it as close as possible to the given value,
     * possibly with animation, and returns the actual zoom that will be reached.
     */
    double setZoom(double zoom);

    /** Gets the point currently shown at the center of the map view. */
    @NonNull MapPoint getCenter();

    /** Pans the map view to center on the given point, possibly with animation. */
    void setCenter(@Nullable MapPoint center);

    /**
     * Adjusts the map's viewport and zoom level to enclose all of the given
     * points.  A scaleFactor of 1.0 ensures that all the points will be just
     * visible in the viewport; a scaleFactor less than 1 shrinks the view
     * beyond that.  For example, a scaleFactor of 0.8 causes the bounding box
     * to occupy at most 80% of the width and 80% of the height of the viewport,
     * ensuring a margin of at least 10% on all sides.
     */
    void zoomToBoundingBox(Iterable<MapPoint> points, double scaleFactor);

    /**
     * Adds a polygonal shape to the map with the given sequence of vertices.
     * The polygon's vertices will have handles that can be dragged by the user.
     * Returns a positive integer, the featureId for the newly added polyline.
     */
    int addDraggableShape(Iterable<MapPoint> points);

    /** Appends a vertex to the polygonal shape specified by featureId. */
    void appendPointToShape(int featureId, @NonNull MapPoint point);

    /**
     * Returns the vertices of the polygonal shape specified by featureId, or an
     * empty list if the featureId does not identify an existing polygonal shape.
     */
    @NonNull List<MapPoint> getPointsOfShape(int featureId);

    /** Removes a specified map feature from the map. */
    void removeFeature(int featureId);

    /** Removes all map features from the map. */
    void clearFeatures();

    /**
     * Enables/disables GPS tracking.  While enabled, the GPS location is shown
     * on the map, and fixes are passed to the listener (setGpsLocationListener).
     * The activity is responsible for disabling GPS location if it doesn't want
     * to keep getting callbacks after it has been stopped or destroyed.
     */
    void setGpsLocationEnabled(boolean enabled);

    /** Gets the last GPS location fix, or null if there hasn't been one. */
    @Nullable MapPoint getGpsLocation();

    /** Registers a callback for GPS location fixes. */
    void setGpsLocationListener(@Nullable PointListener listener);

    /** Registers a callback for a click on the map. */
    void setClickListener(@Nullable PointListener listener);

    /** Registers a callback for a long press on the map. */
    void setLongPressListener(@Nullable PointListener listener);

    interface ReadyListener {
        void onReady(@Nullable MapFragment mapFragment);
    }

    interface PointListener {
        void onPoint(@NonNull MapPoint point);
    }
}
