package org.odk.collect.android.map;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Line;
import com.mapbox.mapboxsdk.plugins.annotation.LineManager;
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions;
import com.mapbox.mapboxsdk.plugins.annotation.OnLineDragListener;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.TransitionOptions;
import com.mapbox.mapboxsdk.utils.ColorUtils;

import org.odk.collect.android.R;
import org.odk.collect.android.preferences.GeneralKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import timber.log.Timber;

import static android.os.Looper.getMainLooper;

public class MapboxMapFragment extends MapboxSdkMapFragment implements MapFragment,
    OnMapReadyCallback, PermissionsListener,
    MapboxMap.OnMapClickListener, MapboxMap.OnMapLongClickListener,
    LocationEngineCallback<LocationEngineResult> {

    public static final LatLng INITIAL_CENTER = new LatLng(0, -30);
    public static final float INITIAL_ZOOM = 2;
    public static final float POINT_ZOOM = 16;
    public static final String POINT_ICON_ID = "point-icon-id";
    public static final long LOCATION_INTERVAL_MILLIS = 1000;
    public static final long LOCATION_MAX_WAIT_MILLIS = 5 * LOCATION_INTERVAL_MILLIS;

    protected MapboxMap map;
    protected ReadyListener readyListener;
    protected List<MapFragment.ReadyListener> gpsLocationReadyListeners = new ArrayList<>();
    protected MapFragment.PointListener gpsLocationListener;
    protected MapFragment.PointListener clickListener;
    protected MapFragment.PointListener longPressListener;
    protected FeatureListener dragEndListener;

    protected PermissionsManager permissionsManager = new PermissionsManager(this);
    protected LocationComponent locationComponent;
    protected boolean locationEnabled;
    protected MapPoint lastLocationFix;

    protected int nextFeatureId = 1;
    protected Map<Integer, MapFeature> features = new HashMap<>();
    protected SymbolManager symbolManager;
    protected LineManager lineManager;

    // During Robolectric tests, Google Play Services is unavailable; sadly, the
    // "map" field will be null and many operations will need to be stubbed out.
    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "This flag is exposed for Robolectric tests to set")
    @VisibleForTesting public static boolean testMode;

    @SuppressWarnings({"MissingPermission"})
    @Override public void addTo(@NonNull FragmentActivity activity, int containerId, @Nullable ReadyListener listener) {
        readyListener = listener;
        activity.getSupportFragmentManager()
            .beginTransaction().replace(containerId, this).commitNow();
        getMapAsync(map -> {
            this.map = map;  // signature of getMapAsync() ensures map is never null
            assert mapView != null;  // should have been initialized by now

            map.setStyle(getDesiredMapboxStyle(), style -> {
                map.getUiSettings().setCompassGravity(Gravity.TOP | Gravity.START);
                map.getUiSettings().setCompassMargins(36, 36, 36, 36);
                map.getStyle().setTransition(new TransitionOptions(0, 0, false));

                Drawable pointIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_map_point);
                map.getStyle().addImage(POINT_ICON_ID, pointIcon);

                map.addOnMapClickListener(this);
                map.addOnMapLongClickListener(this);

                // MAPBOX ISSUE: Only the last-created manager gets draggable annotations.
                // https://github.com/mapbox/mapbox-plugins-android/issues/863
                // For symbols to be draggable, SymbolManager must be created last.
                lineManager = createLineManager();
                symbolManager = createSymbolManager();

                enableLocationComponent();

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(INITIAL_CENTER, INITIAL_ZOOM));
                if (readyListener != null) {
                    readyListener.onReady(this);
                }
            });

            // In Robolectric tests, getMapAsync() never gets around to calling its
            // callback; we have to invoke the ready listener directly.
            if (testMode) {
                readyListener.onReady(this);
            }
        });
    }

    @Override public Fragment getFragment() {
        return this;
    }

    private String getDesiredMapboxStyle() {
        switch (PreferenceManager.getDefaultSharedPreferences(getContext()).getString(
            GeneralKeys.KEY_MAP_BASEMAP, null)) {
            case "mapbox_light":
                return Style.LIGHT;
            case "mapbox_dark":
                return Style.DARK;
            case "mapbox_satellite":
                return Style.SATELLITE;
            case "mapbox_satellite_streets":
                return Style.SATELLITE_STREETS;
            case "mapbox_outdoors":
                return Style.OUTDOORS;
            default:
                return Style.MAPBOX_STREETS;
        }
    }

    private SymbolManager createSymbolManager() {
        SymbolManager symbolManager = new SymbolManager(mapView, map, map.getStyle());
        // MAPBOX ISSUE: Even after setIconAllowOverlap(true), the symbol is not drawn
        // when it is close to other symbols managed by this manager.
        // MAPBOX ISSUE: Even after setIconIgnorePlacement(true), symbols from the
        // basemap disappear when they are close to symbols added with this manager.
        symbolManager.setIconAllowOverlap(true);
        symbolManager.setIconIgnorePlacement(true);
        symbolManager.setIconPadding(0f);
        symbolManager.addDragListener(new OnSymbolDragListener() {
            @Override public void onAnnotationDragStarted(Symbol symbol) {
                updateFeature(findFeature(symbol));
            }

            @Override public void onAnnotationDrag(Symbol symbol) {
                // When a symbol is manually dragged, the position is no longer
                // obtained from a GPS reading, so the altitude and standard
                // deviation fields are no longer meaningful; reset them to zero.
                symbol.setTextField("0;0");
                updateFeature(findFeature(symbol));
            }

            @Override public void onAnnotationDragFinished(Symbol symbol) {
                int featureId = findFeature(symbol);
                updateFeature(featureId);
                if (dragEndListener != null && featureId != -1) {
                    dragEndListener.onFeature(featureId);
                }
                symbol.setDraggable(false);
            }
        });
        return symbolManager;
    }

    private LineManager createLineManager() {
        LineManager lineManager = new LineManager(mapView, map, map.getStyle());
        lineManager.addDragListener(new OnLineDragListener() {
            @Override public void onAnnotationDragStarted(Line annotation) { }

            @Override public void onAnnotationDrag(Line annotation) { }

            @Override public void onAnnotationDragFinished(Line annotation) { }
        });
        return lineManager;
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent() {
        if (map == null) {  // map is null during Robolectric tests
            return;
        }

        // Request permissions if necessary.
        if (!PermissionsManager.areLocationPermissionsGranted(getContext())) {
            permissionsManager.requestLocationPermissions(getActivity());
            return;
        }

        LocationEngineRequest request = new LocationEngineRequest.Builder(LOCATION_INTERVAL_MILLIS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(LOCATION_MAX_WAIT_MILLIS)
            .build();

        LocationEngine engine = LocationEngineProvider.getBestLocationEngine(getContext());
        engine.requestLocationUpdates(request, this, getMainLooper());
        engine.getLastLocation(this);

        locationComponent = map.getLocationComponent();
        if (map.getStyle() != null) {
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(getContext(), map.getStyle())
                    .locationEngine(engine)
                    .locationComponentOptions(
                        LocationComponentOptions.builder(getContext())
                            .foregroundDrawable(R.drawable.ic_crosshairs)
                            .backgroundDrawable(R.drawable.empty)
                            .enableStaleState(false)
                            .elevation(0)  // removes the shadow
                            .build()
                    )
                    .build()
            );
        }

        locationComponent.setCameraMode(CameraMode.NONE);
        locationComponent.setRenderMode(RenderMode.NORMAL);
        locationComponent.setLocationComponentEnabled(locationEnabled);
    }


    // TOOD(ping): This method is only used by MapHelper.  Remove this after
    // MapFragment adds support for selectable basemaps.
    public MapboxMap getMapboxMap() {
        return map;
    }

    @Override public @NonNull MapPoint getCenter() {
        if (map == null) {  // during Robolectric tests, map will be null
            return fromLatLng(INITIAL_CENTER);
        }
        return fromLatLng(map.getCameraPosition().target);
    }

    @Override public double getZoom() {
        if (map == null) {  // during Robolectric tests, map will be null
            return INITIAL_ZOOM;
        }
        return map.getCameraPosition().zoom;
    }

    @Override public void setCenter(@Nullable MapPoint center, boolean animate) {
        if (map == null) {  // during Robolectric tests, map will be null
            return;
        }
        if (center != null) {
            moveOrAnimateCamera(CameraUpdateFactory.newLatLng(toLatLng(center)), animate);
        }
    }

    @Override public void zoomToPoint(@Nullable MapPoint center, boolean animate) {
        zoomToPoint(center, POINT_ZOOM, animate);
    }

    @Override public void zoomToPoint(@Nullable MapPoint center, double zoom, boolean animate) {
        if (map == null) {  // during Robolectric tests, map will be null
            return;
        }
        if (center != null) {
            moveOrAnimateCamera(
                CameraUpdateFactory.newLatLngZoom(toLatLng(center), (float) zoom), animate);
        }
    }

    protected void moveOrAnimateCamera(CameraUpdate movement, boolean animate) {
        if (animate) {
            map.animateCamera(movement);
        } else {
            map.moveCamera(movement);
        }
    }

    @Override public void zoomToBoundingBox(Iterable<MapPoint> points, double scaleFactor, boolean animate) {
        if (map == null) {  // during Robolectric tests, map will be null
            return;
        }
        if (points != null) {
            int count = 0;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            MapPoint lastPoint = null;
            for (MapPoint point : points) {
                lastPoint = point;
                builder.include(toLatLng(point));
                count++;
            }
            if (count == 1) {
                zoomToPoint(lastPoint, animate);
            } else if (count > 1) {
                final LatLngBounds bounds = expandBounds(builder.build(), 1/scaleFactor);
                new Handler().postDelayed(() -> {
                    moveOrAnimateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0), animate);
                }, 100);
            }
        }
    }

    protected Symbol createSymbol(SymbolManager symbolManager, MapPoint point, boolean draggable) {
        // MAPBOX ISSUE: When dragging is enabled, every drag action also adds
        // a point.  The Mapbox SDK seems to provide no way to prevent the drag
        // touch from also passing through to the map and being interpreted as a
        // map click.  This makes dragging unusable, so let's disable it for now.
        draggable = false;

        // A Symbol's position is a LatLng with just latitude and longitude
        // fields.  The point's altitude and standard deviation values need to
        // be stored somewhere, so we put them in an invisible text field.
        return symbolManager.create(new SymbolOptions()
            .withLatLng(toLatLng(point))
            .withIconImage(POINT_ICON_ID)
            .withIconSize(1f)
            .withZIndex(10)
            .withDraggable(draggable)
            .withTextField(point.alt + ";" + point.sd)
            .withTextOpacity(0f)
        );
    }

    /** Finds the feature to which the given symbol belongs. */
    protected int findFeature(Symbol symbol) {
        for (int featureId : features.keySet()) {
            if (features.get(featureId).ownsSymbol(symbol)) {
                return featureId;
            }
        }
        return -1;  // not found
    }

    protected void updateFeature(int featureId) {
        MapFeature feature = features.get(featureId);
        if (feature != null) {
            feature.update();
        }
    }

    @Override public int addMarker(MapPoint point, boolean draggable) {
        int featureId = nextFeatureId++;
        features.put(featureId, new MarkerFeature(symbolManager, point, draggable));
        return featureId;
    }

    @Override public @Nullable MapPoint getMarkerPoint(int featureId) {
        MapFeature feature = features.get(featureId);
        return feature instanceof MarkerFeature ?
            ((MarkerFeature) feature).getPoint() : null;
    }

    @Override public @NonNull List<MapPoint> getPolyPoints(int featureId) {
        MapFeature feature = features.get(featureId);
        if (feature instanceof PolyFeature) {
            return ((PolyFeature) feature).getPoints();
        }
        return new ArrayList<>();
    }

    @Override public void setDragEndListener(@Nullable FeatureListener listener) {
        dragEndListener = listener;
    }

    @Override public @Nullable String getLocationProvider() {
        return null;
    }

    @Override public boolean onMapClick(@NonNull LatLng point) {
        if (clickListener != null) {
            clickListener.onPoint(fromLatLng(point));
        }
        return true;
    }

    protected LatLngBounds expandBounds(LatLngBounds bounds, double factor) {
        double north = bounds.getNorthEast().getLatitude();
        double south = bounds.getSouthWest().getLatitude();
        double latCenter = (north + south)/2;
        double latRadius = ((north - south)/2)*factor;
        north = Math.min(90, latCenter + latRadius);
        south = Math.max(-90, latCenter - latRadius);

        double east = bounds.getNorthEast().getLongitude();
        double west = bounds.getSouthWest().getLongitude();
        while (east < west) {
            east += 360;
        }
        double lonCenter = (east + west)/2;
        double lonRadius = Math.min(180 - 1e-6, ((east - west)/2)*factor);
        east = lonCenter + lonRadius;
        west = lonCenter - lonRadius;

        return new LatLngBounds.Builder()
            .include(new LatLng(south, west))
            .include(new LatLng(north, east))
            .build();
    }

    @Override public int addDraggablePoly(@NonNull Iterable<MapPoint> points, boolean closedPolygon) {
        int featureId = nextFeatureId++;
        features.put(featureId, new PolyFeature(lineManager, symbolManager, points, closedPolygon));
        return featureId;
    }

    @Override public void appendPointToPoly(int featureId, @NonNull MapPoint point) {
        MapFeature feature = features.get(featureId);
        if (feature instanceof PolyFeature) {
            ((PolyFeature) feature).addPoint(point);
        }
    }

    @Override public void removePolyLastPoint(int featureId) {
        MapFeature feature = features.get(featureId);
        if (feature instanceof PolyFeature) {
            ((PolyFeature) feature).removeLastPoint();
        }
    }

    @Override public void removeFeature(int featureId) {
        MapFeature feature = features.get(featureId);
        features.remove(feature);
        if (feature != null) {
            feature.dispose();
        }
    }

    @Override public void clearFeatures() {
        if (map != null) {  // during Robolectric tests, map will be null
            for (MapFeature feature : features.values()) {
                feature.dispose();
            }
        }
        features.clear();
    }

    @Override public void setClickListener(@Nullable PointListener listener) {
        clickListener = listener;
    }

    @Override public void setLongPressListener(@Nullable PointListener listener) {
        longPressListener = listener;
    }

    @Override public void setGpsLocationListener(@Nullable PointListener listener) {
        gpsLocationListener = listener;
    }

    @Override public void setGpsLocationEnabled(boolean enable) {
        locationEnabled = enable;
        if (locationComponent != null) {
            locationComponent.setLocationComponentEnabled(enable);
        }
    }

    @Override public void runOnGpsLocationReady(@NonNull ReadyListener listener) {
        if (lastLocationFix != null) {
            listener.onReady(this);
        } else {
            gpsLocationReadyListeners.add(listener);
        }
    }

    @Override public @Nullable MapPoint getGpsLocation() {
        return lastLocationFix;
    }

    @Override public boolean onMapLongClick(@NonNull LatLng latLng) {
        if (longPressListener != null) {
            longPressListener.onPoint(fromLatLng(latLng));
        }
        return true;
    }

    @Override public void onSuccess(LocationEngineResult result) {
        lastLocationFix = fromLocation(result.getLastLocation());
        if (locationComponent != null) {
            locationComponent.forceLocationUpdate(result.getLastLocation());
        }
        for (ReadyListener listener : gpsLocationReadyListeners) {
            listener.onReady(this);
        }
        gpsLocationReadyListeners.clear();
        if (gpsLocationListener != null) {
            gpsLocationListener.onPoint(lastLocationFix);
        }
    }

    @Override public void onFailure(@NonNull Exception exception) { }

    protected static @NonNull MapPoint fromLatLng(@NonNull LatLng latLng) {
        return new MapPoint(latLng.getLatitude(), latLng.getLongitude());
    }

    protected static @Nullable MapPoint fromLocation(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        return new MapPoint(location.getLatitude(), location.getLongitude(),
            location.getAltitude(), location.getAccuracy());
    }

    protected static @NonNull MapPoint fromSymbol(@NonNull Symbol symbol) {
        LatLng position = symbol.getLatLng();
        String text = symbol.getTextField();
        String[] parts = (text != null ? text : "").split(";");
        double alt = 0;
        double sd = 0;
        try {
            if (parts.length >= 1) {
                alt = Double.parseDouble(parts[0]);
            }
            if (parts.length >= 2) {
                sd = Double.parseDouble(parts[1]);
            }
        } catch (NumberFormatException e) {
            Timber.w("Symbol.getTextField() did not contain two numbers");
        }
        return new MapPoint(position.getLatitude(), position.getLongitude(), alt, sd);
    }

    protected static @NonNull LatLng toLatLng(@NonNull MapPoint point) {
        return new LatLng(point.lat, point.lon);
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getContext(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent();
        } else {
            Toast.makeText(getContext(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * A MapFeature is a physical feature on a map, such as a point, a road,
     * a building, a region, etc.  It is presented to the user as one editable
     * object, though its appearance may be constructed from multiple overlays
     * (e.g. geometric elements, handles for manipulation, etc.).
     */
    interface MapFeature {
        /** Returns true if the given Symbol belongs to this feature. */
        boolean ownsSymbol(Symbol symbol);

        /** Updates the feature's geometry after any UI handles have moved. */
        void update();

        /** Removes the feature from the map, leaving it no longer usable. */
        void dispose();
    }

    /** A Symbol that can optionally be dragged by the user. */
    protected class MarkerFeature implements MapFeature {
        private SymbolManager symbolManager;
        private Symbol symbol;

        public MarkerFeature(SymbolManager symbolManager, MapPoint point, boolean draggable) {
            this.symbolManager = symbolManager;
            symbol = createSymbol(symbolManager, point, draggable);
        }

        public MapPoint getPoint() {
            return fromSymbol(symbol);
        }

        @Override public boolean ownsSymbol(Symbol symbol) {
            return symbol.getId() == this.symbol.getId();
        }

        public void update() { }

        public void dispose() {
            symbolManager.delete(symbol);
            symbol = null;
        }
    }

    /** A polyline or polygon that can be manipulated by dragging Symbols at its vertices. */
    protected class PolyFeature implements MapFeature {
        final LineManager lineManager;
        final SymbolManager symbolManager;
        final List<Symbol> symbols = new ArrayList<>();
        final boolean closedPolygon;
        Line line;
        public static final float STROKE_WIDTH = 5;

        public PolyFeature(LineManager lineManager, SymbolManager symbolManager,
            Iterable<MapPoint> points, boolean closedPolygon) {
            this.lineManager = lineManager;
            this.symbolManager = symbolManager;
            this.closedPolygon = closedPolygon;
            for (MapPoint point : points) {
                symbols.add(createSymbol(symbolManager, point, true));
            }
            update();
        }

        @Override public boolean ownsSymbol(Symbol givenSymbol) {
            long givenId = givenSymbol.getId();
            for (Symbol symbol : symbols) {
                if (symbol.getId() == givenId) {
                    return true;
                }
            }
            return false;
        }

        public void update() {
            List<LatLng> latLngs = new ArrayList<>();
            for (Symbol symbol : symbols) {
                latLngs.add(toLatLng(fromSymbol(symbol)));
            }
            if (closedPolygon && !latLngs.isEmpty()) {
                latLngs.add(latLngs.get(0));
            }
            if (latLngs.isEmpty()) {
                clearLine();
            } else if (line == null) {
                line = lineManager.create(new LineOptions()
                    .withLineColor(ColorUtils.colorToRgbaString(getResources().getColor(R.color.mapLine)))
                    .withLatLngs(latLngs)
                    .withLineWidth(STROKE_WIDTH)
                );
            } else {
                line.setLatLngs(latLngs);
                lineManager.update(line);
            }
        }

        public void dispose() {
            clearLine();
            for (Symbol symbol : symbols) {
                symbolManager.delete(symbol);
            }
            symbols.clear();
        }

        public List<MapPoint> getPoints() {
            List<MapPoint> points = new ArrayList<>();
            for (Symbol symbol : symbols) {
                points.add(fromSymbol(symbol));
            }
            return points;
        }

        public void addPoint(MapPoint point) {
            if (map == null) {  // during Robolectric tests, map will be null
                return;
            }
            symbols.add(createSymbol(symbolManager, point, true));
            update();
        }

        public void removeLastPoint() {
            if (!symbols.isEmpty()) {
                int last = symbols.size() - 1;
                symbolManager.delete(symbols.get(last));
                symbols.remove(last);
                update();
            }
        }

        protected void clearLine() {
            if (line != null) {
                lineManager.delete(line);
                line = null;
            }
        }
    }
}
