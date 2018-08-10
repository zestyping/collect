/*
 * Copyright (C) 2016 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.map.GoogleMapFragment;
import org.odk.collect.android.map.MapFragment;
import org.odk.collect.android.map.MapPoint;
import org.odk.collect.android.spatial.MapHelper;
import org.odk.collect.android.utilities.ToastUtils;
import org.odk.collect.android.widgets.GeoShapeWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Activity for entering or editing a polygon on a map. */
public class GeoShapeGoogleMapActivity extends CollectAbstractActivity {
    private MapFragment map;
    private int shapeId = -1;  // will be a positive featureId once map is ready
    private ImageButton gpsButton;
    private ImageButton clearButton;

    private MapHelper helper;
    private AlertDialog zoomDialog;
    private View zoomDialogView;
    private Button zoomPointButton;
    private Button zoomLocationButton;
    private boolean foundFirstLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.geoshape_layout);

        // TODO(ping): Remove when we're ready to use this class.
        ((TextView) findViewById(R.id.top_text)).setText("new GeoShapeActivity");

        createMapFragment().addTo(this, R.id.map_container, this::setupMap);
    }

    // TODO(ping): Select the appropriate MapFragment implementation (Google or OSM).
    public static MapFragment createMapFragment() {
        return new GoogleMapFragment();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Collect.getInstance().getActivityLogger().logOnStart(this);
        if (map != null) map.setGpsLocationEnabled(true);
    }

    @Override
    protected void onStop() {
        map.setGpsLocationEnabled(false);
        Collect.getInstance().getActivityLogger().logOnStop(this);
        super.onStop();
    }

    private void setupMap(MapFragment newMapFragment) {
        if (newMapFragment == null) {
            finish();
            return;
        }

        map = newMapFragment;
        map.setGpsLocationEnabled(true);
        map.setGpsLocationListener(this::onLocationFix);
        map.setLongPressListener(this::addVertex);

        helper = new MapHelper(this, newMapFragment);

        gpsButton = findViewById(R.id.gps);
        gpsButton.setOnClickListener(v -> showZoomDialog());

        clearButton = findViewById(R.id.clear);
        clearButton.setOnClickListener(v -> showClearDialog());

        ImageButton saveButton = findViewById(R.id.save);
        saveButton.setOnClickListener(v -> finishWithResult());

        List<MapPoint> points = new ArrayList<>();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(GeoShapeWidget.SHAPE_LOCATION)) {
            points = parsePoints(intent.getStringExtra(GeoShapeWidget.SHAPE_LOCATION));
        }
        shapeId = map.addDraggableShape(points);
        gpsButton.setEnabled(!points.isEmpty());
        clearButton.setEnabled(!points.isEmpty());

        ImageButton layersButton = findViewById(R.id.layers);
        layersButton.setOnClickListener(v -> helper.showLayersDialog(GeoShapeGoogleMapActivity.this));

        zoomDialogView = getLayoutInflater().inflate(R.layout.geo_zoom_dialog, null);

        zoomLocationButton = zoomDialogView.findViewById(R.id.zoom_location);
        zoomLocationButton.setOnClickListener(v -> {
            map.setCenter(map.getGpsLocation());
            zoomDialog.dismiss();
        });

        zoomPointButton = zoomDialogView.findViewById(R.id.zoom_saved_location);
        zoomPointButton.setOnClickListener(v -> {
            map.zoomToBoundingBox(map.getPointsOfShape(shapeId), 0.8);
            zoomDialog.dismiss();
        });

        // If there is a last know location go there
        if (hasWindowFocus() && map.getGpsLocation() != null) {
            foundFirstLocation = true;
            gpsButton.setEnabled(true);
            showZoomDialog();
        }

        helper.setBasemap();
    }

    private void finishWithResult() {
        List<MapPoint> points = map.getPointsOfShape(shapeId);
        if (points.size() < 3) {
            ToastUtils.showShortToastInMiddle(getString(R.string.polygon_validator));
            return;
        }

        setResult(RESULT_OK, new Intent().putExtra(
            FormEntryActivity.GEOSHAPE_RESULTS, formatPoints(points)));
        finish();
    }

    /**
     * Parses a form result string, as previously formatted by formatPoints,
     * into a list of polygon vertices.
     */
    private List<MapPoint> parsePoints(String coords) {
        List<MapPoint> points = new ArrayList<>();
        for (String vertex : (coords == null ? "" : coords).split(";")) {
            String[] words = vertex.trim().split(" ");
            if (words.length < 2) continue;
            double lat, lon;
            try {
                lat = Double.parseDouble(words[0]);
                lon = Double.parseDouble(words[1]);
            } catch (NumberFormatException e) {
                continue;
            }
            points.add(new MapPoint(lat, lon));
        }
        // Polygons are stored with a last point that duplicates the first
        // point.  To prepare the polygon for display and editing, we need
        // to remove this duplicate point.
        int count = points.size();
        if (count > 1 && points.get(0).equals(points.get(count - 1))) {
            points.remove(count - 1);
        }
        return points;
    }

    /**
     * Serializes a list of polygon vertices into a string, in the format
     * appropriate for storing as the result of this form question.
     */
    private String formatPoints(List<MapPoint> points) {
        String result = "";
        if (points.size() > 1) {
            // Polygons are stored with a last point that duplicates the
            // first point.  Add this extra point if it's not already present.
            if (!points.get(0).equals(points.get(points.size() - 1))) {
                points.add(points.get(0));
            }
            for (MapPoint point : points) {
                result += String.format(Locale.US, "%.6f %.6f 0.0 0.0;", point.lat, point.lon);
            }
        }
        return result;
    }

    private void onLocationFix(MapPoint point) {
        gpsButton.setEnabled(true);
        if (hasWindowFocus() && !foundFirstLocation) {
            foundFirstLocation = true;
            showZoomDialog();
        }
    }

    private void addVertex(MapPoint point) {
        map.appendPointToShape(shapeId, point);
        clearButton.setEnabled(true);
    }

    private void clear() {
        map.clearFeatures();
        shapeId = map.addDraggableShape(new ArrayList<>());
        map.setLongPressListener(this::addVertex);
        clearButton.setEnabled(false);
    }

    private void showClearDialog() {
        if (!map.getPointsOfShape(shapeId).isEmpty()) {
            new AlertDialog.Builder(this)
                .setMessage(getString(R.string.geo_clear_warning))
                .setPositiveButton(getString(R.string.clear), (dialog, id) -> clear())
                .setNegativeButton(R.string.cancel, null)
                .show();
        }
    }

    public void showZoomDialog() {
        if (zoomDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.zoom_to_where));
            builder.setView(zoomDialogView)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.cancel();
                        zoomDialog.dismiss();
                    }
                });
            zoomDialog = builder.create();
        }

        if (zoomLocationButton != null) {
            if (map.getGpsLocation() != null) {
                zoomLocationButton.setEnabled(true);
                zoomLocationButton.setBackgroundColor(Color.parseColor("#50cccccc"));
                zoomLocationButton.setTextColor(themeUtils.getPrimaryTextColor());
            } else {
                zoomLocationButton.setEnabled(false);
                zoomLocationButton.setBackgroundColor(Color.parseColor("#50e2e2e2"));
                zoomLocationButton.setTextColor(Color.parseColor("#FF979797"));
            }

            if (!map.getPointsOfShape(shapeId).isEmpty()) {
                zoomPointButton.setEnabled(true);
                zoomPointButton.setBackgroundColor(Color.parseColor("#50cccccc"));
                zoomPointButton.setTextColor(themeUtils.getPrimaryTextColor());
            } else {
                zoomPointButton.setEnabled(false);
                zoomPointButton.setBackgroundColor(Color.parseColor("#50e2e2e2"));
                zoomPointButton.setTextColor(Color.parseColor("#FF979797"));
            }
        }

        zoomDialog.show();
    }

    public ImageButton getGpsButton() {
        return gpsButton;
    }

    @VisibleForTesting public AlertDialog getZoomDialog() {
        return zoomDialog;
    }

    @VisibleForTesting public MapFragment getMapFragment() { return map; }
}
