/*
 * Copyright (C) 2015 GeoODK
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import org.odk.collect.android.R;
import org.odk.collect.android.spatial.MapHelper;
import org.odk.collect.android.widgets.GeoTraceWidget;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class GeoTraceOsmMapActivity extends Activity implements IRegisterReceiver,
        LocationListener {
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture schedulerHandler;

    public Boolean gpsStatus = true;
    private Boolean playCheck = false;
    private MapView mapView;
    public MyLocationNewOverlay myLocationOverlay;
    private TextView locationStatus;
    private TextView collectionStatus;
    private ImageButton locationButton;
    private ImageButton playButton;
    private ImageButton backspaceButton;
    public ImageButton layersButton;
    public ImageButton clearButton;
    private Button manualCaptureButton;
    private ImageButton pauseButton;
    public AlertDialog.Builder builder;
    public AlertDialog.Builder polylineAlertBuilder;
    public LayoutInflater inflater;
    private AlertDialog alert;
    private AlertDialog alertDialog;
    private View traceSettingsView;
    private View polygonPolylineView;
    private Polyline polyline;
    private List<Marker> mapMarkers = new ArrayList<>();
    private List<Location> locations = new ArrayList<>();
    private Integer traceMode; // 0 manual, 1 is automatic
    private View traceAutoOptions;
    private Spinner autoInterval;
    private long autoIntervalSeconds;
    private Spinner autoAccuracy;
    private double autoAccuracyMeters;
    private Boolean beenPaused;
    private MapHelper helper;

    private AlertDialog zoomDialog;
    private View zoomDialogView;
    private Button zoomPointButton;
    private Button zoomLocationButton;
    private Boolean modeActive = false;
    private Boolean gpsOn = false;
    private Boolean networkOn = false;

    private final String GPS_PROVIDER = "gps";
    private final int TRACE_MODE_MANUAL = 0;
    private final int TRACE_MODE_AUTO = 1;
    private final int ZOOM_LEVEL_NO_GPS_FIX = 3;
    private final int ZOOM_LEVEL_WITH_GPS_FIX = 19;
    private final int ZOOM_LEVEL_DURING_RECORDING = 21;
    private final int MAX_ZOOM_LEVEL = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.geotrace_osm_layout);
        setTitle(getString(R.string.geotrace_title)); // Setting title of the action

        mapView = (MapView) findViewById(R.id.geotrace_mapview);
        helper = new MapHelper(this, mapView, GeoTraceOsmMapActivity.this);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(ZOOM_LEVEL_NO_GPS_FIX);
        mapView.setMaxZoomLevel(MAX_ZOOM_LEVEL);

        myLocationOverlay = new MyLocationNewOverlay(mapView);
        Drawable crosshairs = ContextCompat.getDrawable(getApplicationContext(), R.drawable.crosshairs);
        Bitmap crosshairsBitmap = ((BitmapDrawable) crosshairs).getBitmap();
        myLocationOverlay.setDirectionArrow(crosshairsBitmap, crosshairsBitmap);

        locationStatus = (TextView) findViewById(R.id.geotrace_location_status);
        collectionStatus = (TextView) findViewById(R.id.geotrace_collection_status);

        inflater = this.getLayoutInflater();
        traceSettingsView = inflater.inflate(R.layout.geotrace_dialog_osm, null);
        polygonPolylineView = inflater.inflate(R.layout.polygon_polyline_dialog, null);
        autoInterval = (Spinner) traceSettingsView.findViewById(R.id.trace_osm_auto_interval);
        autoInterval.setSelection(1);  // default to a 5-second interval
        autoAccuracy = (Spinner) traceSettingsView.findViewById(R.id.trace_osm_auto_accuracy);
        autoAccuracy.setSelection(1);  // default to requiring 5-meter accuracy
        autoAccuracyMeters = 5;
        traceAutoOptions = traceSettingsView.findViewById(R.id.trace_osm_auto_options);
        layersButton = (ImageButton) findViewById(R.id.layers);
        layersButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                helper.showLayersDialog(GeoTraceOsmMapActivity.this);

            }
        });

        locationButton = (ImageButton) findViewById(R.id.show_location);
        locationButton.setEnabled(false);
        locationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                reset_trace_settings();
                zoomToMyLocation();
            }

        });

        backspaceButton = (ImageButton) findViewById(R.id.backspace);
        backspaceButton.setEnabled(false);
        backspaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeLastPoint();
            }
        });

        clearButton = (ImageButton) findViewById(R.id.clear);
        clearButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showClearDialog();

            }

        });

        ImageButton saveButton = (ImageButton) findViewById(R.id.geotrace_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveGeoTrace();
            }
        });

        clearButton.setEnabled(!mapMarkers.isEmpty());
        manualCaptureButton = (Button) findViewById(R.id.manual_button);
        manualCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPoint();
            }
        });
        pauseButton = (ImageButton) findViewById(R.id.pause);
        playButton = (ImageButton) findViewById(R.id.play);
        playButton.setEnabled(false);
        beenPaused = false;
        traceMode = 1;

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (!playCheck) {
                    if (!beenPaused) {
                        alert.show();
                    } else {
                        RadioGroup rb = (RadioGroup) traceSettingsView.findViewById(
                                R.id.trace_osm_radio_group);
                        int radioButtonID = rb.getCheckedRadioButtonId();
                        View radioButton = rb.findViewById(radioButtonID);
                        traceMode = rb.indexOfChild(radioButton);
                        if (traceMode == 0) {
                            setupManualMode();
                        } else if (traceMode == 1) {
                            setupAutomaticMode();
                        } else {
                            reset_trace_settings();
                        }
                    }
                    playCheck = true;
                    updateStatusText();
                } else {
                    playCheck = false;
                    startGeoTrace();
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                playButton.setVisibility(View.VISIBLE);
                clearButton.setEnabled(!mapMarkers.isEmpty());
                pauseButton.setVisibility(View.GONE);
                manualCaptureButton.setVisibility(View.GONE);
                playCheck = true;
                modeActive = false;
                myLocationOverlay.disableFollowLocation();
                updateStatusText();

                try {
                    schedulerHandler.cancel(true);
                } catch (Exception e) {
                    // Do nothing
                }
            }
        });

        overlayMapLayerListener();
        buildDialogs();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(GeoTraceWidget.TRACE_LOCATION)) {
            initializeTraceFromIntent(intent.getStringExtra(GeoTraceWidget.TRACE_LOCATION));
            zoomToBounds();
            playButton.setEnabled(false);
            clearButton.setEnabled(true);
            locationButton.setEnabled(true);
        }
        myLocationOverlay.runOnFirstFix(centerAroundFix);

        Button polylineSaveButton = (Button) polygonPolylineView.findViewById(R.id.polyline_save);
        polylineSaveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                saveGeoTrace();

            }
        });

        zoomDialogView = getLayoutInflater().inflate(R.layout.geoshape_zoom_dialog, null);

        zoomLocationButton = (Button) zoomDialogView.findViewById(R.id.zoom_location);
        zoomLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                zoomToMyLocation();
                mapView.invalidate();
                zoomDialog.dismiss();
            }
        });

        zoomPointButton = (Button) zoomDialogView.findViewById(R.id.zoom_shape);
        zoomPointButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //zoomToCentroid();
                zoomToBounds();
                mapView.invalidate();
                zoomDialog.dismiss();
            }
        });


        mapView.invalidate();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                gpsOn = true;
            }
            if (provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                networkOn = true;
            }
        }
        if (gpsOn) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
        if (networkOn) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        }

        updateStatusText();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            helper.setBasemap();
        }

        updateStatusText();
        upMyLocationOverlayLayers();
    }

    @Override
    protected void onStop() {
        disableMyLocation();
        super.onStop();
    }

    @Override
    public void finish() {
        ViewGroup view = (ViewGroup) getWindow().getDecorView();
        view.removeAllViews();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        if (schedulerHandler != null && !schedulerHandler.isCancelled()) {
            schedulerHandler.cancel(true);
        }
        super.onDestroy();
    }

    public void setGeoTraceScheduler(int intervalSeconds) {
        autoIntervalSeconds = intervalSeconds;
        schedulerHandler = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addPointIfAcceptable();
                    }
                });
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void initializeTraceFromIntent(String wkt) {
        int left = wkt.indexOf("(");
        int right = wkt.lastIndexOf(")");
        if (left < 0 || right < 0) return;

        List<GeoPoint> points = new ArrayList<>();
        for (String element : wkt.substring(left + 1, right).split(",")) {
            String[] values = element.trim().split("\\s+");
            double lat = Double.parseDouble(values[0]);
            double lon = Double.parseDouble(values[1]);
            double alt = Double.parseDouble(values[2]);
            float accuracy = Float.parseFloat(values[3]);

            Location loc = new Location(GPS_PROVIDER);
            loc.setLatitude(lat);
            loc.setLongitude(lon);
            loc.setAltitude(alt);
            loc.setAccuracy(accuracy);
            locations.add(loc);

            GeoPoint point = new GeoPoint(lat, lon);
            point.setAltitude(alt);
            points.add(point);

            Marker marker = new Marker(mapView);
            marker.setSubDescription("" + accuracy);
            marker.setPosition(point);
            marker.setOnMarkerClickListener(nullMarkerListener);
            marker.setDraggable(true);
            marker.setOnMarkerDragListener(dragListener);
            marker.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.point_red));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            mapMarkers.add(marker);
            mapView.getOverlays().add(marker);
        }
        polyline.setPoints(points);
        mapView.invalidate();
    }

    private void disableMyLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            myLocationOverlay.setEnabled(false);
            myLocationOverlay.disableFollowLocation();
            myLocationOverlay.disableMyLocation();
            gpsStatus = false;
        }

    }

    private void upMyLocationOverlayLayers() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            overlayMyLocationLayers();
        } else {
            showGPSDisabledAlertToUser();
        }
    }

    private void overlayMapLayerListener() {
        polyline = new Polyline();
        polyline.setColor(Color.RED);
        Paint paint = polyline.getPaint();
        paint.setStrokeWidth(5);
        mapView.getOverlays().add(polyline);
        mapView.invalidate();
    }

    private void overlayMyLocationLayers() {
        //myLocationOverlay.runOnFirstFix(centerAroundFix);
        //if(myLocationOverlay.getMyLocation()!= null){
        //myLocationOverlay.runOnFirstFix(centerAroundFix);
        //}
        mapView.getOverlays().add(myLocationOverlay);
        myLocationOverlay.setEnabled(true);
        myLocationOverlay.enableMyLocation();


    }

    private Handler handler = new Handler(Looper.getMainLooper());

    private Runnable centerAroundFix = new Runnable() {
        public void run() {
            handler.post(new Runnable() {
                public void run() {
                    locationButton.setEnabled(true);
                    playButton.setEnabled(true);
                    zoomToMyLocation();
                }
            });
        }
    };


    private void zoomToMyLocation() {
        if (myLocationOverlay.getMyLocation() != null) {
            mapView.setMaxZoomLevel(MAX_ZOOM_LEVEL);
            if (mapView.getZoomLevel() < ZOOM_LEVEL_WITH_GPS_FIX) {
                mapView.getController().setZoom(ZOOM_LEVEL_WITH_GPS_FIX);
            }
            mapView.getController().setCenter(myLocationOverlay.getMyLocation());
            myLocationOverlay.enableFollowLocation();
        } else {
            mapView.getController().setZoom(ZOOM_LEVEL_NO_GPS_FIX);
        }
        updateStatusText();
    }

    private void updateStatusText() {
        Location loc = myLocationOverlay.getLastFix();
        boolean usable = loc != null && loc.getAccuracy() < autoAccuracyMeters;
        locationStatus.setText(loc == null ?
            getString(R.string.geotrace_location_status_searching) :
                usable ? getString(R.string.geotrace_location_status_acceptable, loc.getAccuracy()) :
                    getString(R.string.geotrace_location_status_unacceptable, loc.getAccuracy())
        );
        locationStatus.setBackgroundColor(
            loc == null ? Color.parseColor("#333333") :
                usable ? Color.parseColor("#337733") : Color.parseColor("#773333"));

        int numPoints = mapMarkers.size();
        collectionStatus.setText(modeActive ? (
            traceMode == TRACE_MODE_MANUAL ?
                getString(R.string.geotrace_collection_status_manual, numPoints) :
                getString(
                    R.string.geotrace_collection_status_auto, numPoints,
                    autoIntervalSeconds, (int) autoAccuracyMeters)
        ) : getString(R.string.geotrace_collection_status_paused, numPoints));
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(getString(R.string.enable_gps_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.enable_gps),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startActivityForResult(
                                        new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                            }
                        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    // Called when either the "Manual" or "Automatic" radio button is clicked
    public void setGeoTraceMode(View view) {
        if (((RadioButton) view).isChecked()) {
            switch (view.getId()) {
                case R.id.trace_osm_manual:
                    traceMode = TRACE_MODE_MANUAL;
                    break;
                case R.id.trace_osm_auto:
                    traceMode = TRACE_MODE_AUTO;
                    break;
            }
        }
        autoInterval.invalidate();
        traceAutoOptions.setVisibility(
            traceMode == TRACE_MODE_AUTO ? View.VISIBLE : View.GONE);
        updateStatusText();
    }

    private void buildDialogs() {

        builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.select_geotrace_mode));
        builder.setView(null);
        builder.setView(traceSettingsView)
                // Add action buttons
                .setPositiveButton(getString(R.string.start),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                startGeoTrace();
                                dialog.cancel();
                                alert.dismiss();
                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        alert.dismiss();
                        reset_trace_settings();
                    }
                })
                .setOnCancelListener(new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        reset_trace_settings();
                    }
                });


        alert = builder.create();


        polylineAlertBuilder = new AlertDialog.Builder(this);
        polylineAlertBuilder.setTitle(getString(R.string.polyline_polygon_text));
        polylineAlertBuilder.setView(polygonPolylineView)
                // Add action buttons
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();

                    }
                })
                .setOnCancelListener(new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.cancel();
                        alert.dismiss();
                    }
                });

        alertDialog = polylineAlertBuilder.create();
    }


    private void reset_trace_settings() {
        playCheck = false;
    }

    private void startGeoTrace() {
        RadioGroup rb = (RadioGroup) traceSettingsView.findViewById(R.id.trace_osm_radio_group);
        int radioButtonID = rb.getCheckedRadioButtonId();
        View radioButton = rb.findViewById(radioButtonID);
        int idx = rb.indexOfChild(radioButton);
        beenPaused = true;
        traceMode = idx;
        if (traceMode == TRACE_MODE_MANUAL) {
            setupManualMode();
        } else if (traceMode == TRACE_MODE_AUTO) {
            setupAutomaticMode();
        } else {
            reset_trace_settings();
        }
        playButton.setVisibility(View.GONE);
        clearButton.setEnabled(false);
        pauseButton.setVisibility(View.VISIBLE);
        if (mapView.getZoomLevel() < ZOOM_LEVEL_DURING_RECORDING) {
            mapView.getController().setZoom(ZOOM_LEVEL_DURING_RECORDING);
        }
        updateStatusText();
    }

    private void setupManualMode() {
        manualCaptureButton.setVisibility(View.VISIBLE);
        modeActive = true;
    }

    private void setupAutomaticMode() {
        manualCaptureButton.setVisibility(View.VISIBLE);

        TypedArray intervalValues = getResources().obtainTypedArray(R.array.interval_values);
        setGeoTraceScheduler(intervalValues.getInt(autoInterval.getSelectedItemPosition(), 1));

        TypedArray accuracyValues = getResources().obtainTypedArray(R.array.accuracy_values);
        autoAccuracyMeters = accuracyValues.getInt(autoAccuracy.getSelectedItemPosition(), 5);

        modeActive = true;
    }

    /**
     * Records the current location as a path point if it's a GPS fix and its
     * accuracy is good enough.
     */
    private void addPointIfAcceptable() {
        Location loc = myLocationOverlay.getLastFix();
        if (loc != null && GPS_PROVIDER.equals(loc.getProvider()) && loc.getAccuracy() < autoAccuracyMeters) {
            addPoint();
        }
    }

    /** Records the current location as a path point. */
    private void addPoint() {
        Location loc = myLocationOverlay.getLastFix();
        if (loc != null) {
            locations.add(loc);

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(loc));
            marker.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.point_red));
            marker.setSubDescription(Float.toString(loc.getAccuracy()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setDraggable(true);
            marker.setOnMarkerDragListener(dragListener);
            mapMarkers.add(marker);

            marker.setOnMarkerClickListener(nullMarkerListener);
            mapView.getOverlays().add(marker);
            List<GeoPoint> points = polyline.getPoints();
            points.add(marker.getPosition());
            polyline.setPoints(points);
            mapView.invalidate();

            backspaceButton.setEnabled(true);
        }
        updateStatusText();
    }

    private void removeLastPoint() {
        List<GeoPoint> points = polyline.getPoints();
        if (locations.isEmpty() || mapMarkers.isEmpty() || points.isEmpty()) return;

        Marker marker = mapMarkers.get(mapMarkers.size() - 1);
        marker.remove(mapView);

        locations.remove(locations.size() - 1);
        mapMarkers.remove(mapMarkers.size() - 1);
        points.remove(points.size() - 1);

        polyline.setPoints(points);
        mapView.invalidate();

        backspaceButton.setEnabled(!locations.isEmpty());
        updateStatusText();
    }

    private void saveGeoTrace() {
        returnLocation();
        finish();
    }

    private void showPolygonErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.polygon_validator))
                .setPositiveButton(getString(R.string.dialog_continue),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        }).show();

    }


    private String generateReturnString() {
        List<String> elements = new ArrayList<>();
        for (Location loc : locations) {
            elements.add(String.format(Locale.ENGLISH, "%.6f %.6f %d %.1f",
                loc.getLatitude(), loc.getLongitude(), (int) loc.getAltitude(), loc.getAccuracy()));
        }
        return "LINESTRING ZM(" + TextUtils.join(", ", elements) + ")";
    }

    private void returnLocation() {
        String finalReturnString = generateReturnString();
        Intent i = new Intent();
        i.putExtra(
                FormEntryActivity.GEOTRACE_RESULTS,
                finalReturnString);
        setResult(RESULT_OK, i);
    }

    private Marker.OnMarkerClickListener nullMarkerListener = new Marker.OnMarkerClickListener() {

        @Override
        public boolean onMarkerClick(Marker arg0, MapView arg1) {
            return false;
        }
    };

    private void updatePolylineFromMarkers() {
        List<GeoPoint> points = new ArrayList<>();
        for (Marker marker : mapMarkers) {
            points.add(marker.getPosition());
        }
        polyline.setPoints(points);
        mapView.invalidate();
    }

    private Marker.OnMarkerDragListener dragListener = new Marker.OnMarkerDragListener() {
        @Override
        public void onMarkerDragStart(Marker marker) { }

        @Override
        public void onMarkerDragEnd(Marker arg0) {
            updatePolylineFromMarkers();
        }

        @Override
        public void onMarkerDrag(Marker marker) {
            updatePolylineFromMarkers();

        }

    };

    private void showClearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.geo_clear_warning))
                .setPositiveButton(getString(R.string.clear),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                clearFeatures();
                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                }).show();

    }

    private void clearFeatures() {
        mapMarkers.clear();
        polyline.setPoints(new ArrayList<GeoPoint>());
        locations.clear();
        mapView.getOverlays().clear();
        clearButton.setEnabled(false);
        overlayMyLocationLayers();
        overlayMapLayerListener();
        mapView.invalidate();
        playButton.setEnabled(true);
        modeActive = false;
        updateStatusText();
    }

    private void zoomToBounds() {
        mapView.getController().setZoom(4);
        mapView.invalidate();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                double minLat = Double.MAX_VALUE;
                double maxLat = Double.MIN_VALUE;
                double minLon = Double.MAX_VALUE;
                double maxLon = Double.MIN_VALUE;
                if (!mapMarkers.isEmpty()) {
                    for (Marker marker : mapMarkers) {
                        double lat = marker.getPosition().getLatitude();
                        double lon = marker.getPosition().getLongitude();
                        minLat = Math.min(minLat, lat);
                        maxLat = Math.max(maxLat, lat);
                        minLon = Math.min(minLon, lon);
                        maxLon = Math.max(maxLon, lon);
                    }
                    mapView.zoomToBoundingBox(
                        new BoundingBox(maxLat, maxLon, minLat, minLon), false);
                } else {
                    zoomToMyLocation();
                }
                mapView.invalidate();
            }
        }, 100);
        mapView.invalidate();
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

        if (myLocationOverlay.getMyLocation() != null) {
            zoomLocationButton.setEnabled(true);
            zoomLocationButton.setBackgroundColor(Color.parseColor("#50cccccc"));
            zoomLocationButton.setTextColor(Color.parseColor("#ff333333"));
        } else {
            zoomLocationButton.setEnabled(false);
            zoomLocationButton.setBackgroundColor(Color.parseColor("#50e2e2e2"));
            zoomLocationButton.setTextColor(Color.parseColor("#FF979797"));
        }
        //If feature enable zoom to button else disable
        if (mapMarkers.size() != 0) {
            zoomPointButton.setEnabled(true);
            zoomPointButton.setBackgroundColor(Color.parseColor("#50cccccc"));
            zoomPointButton.setTextColor(Color.parseColor("#ff333333"));
        } else {
            zoomPointButton.setEnabled(false);
            zoomPointButton.setBackgroundColor(Color.parseColor("#50e2e2e2"));
            zoomPointButton.setTextColor(Color.parseColor("#FF979797"));
        }
        zoomDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        updateStatusText();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void destroy() { }
}
