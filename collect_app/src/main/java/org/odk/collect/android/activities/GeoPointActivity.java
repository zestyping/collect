/*
 * Copyright (C) 2009 University of Washington
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.utilities.InfoLogger;
import org.odk.collect.android.utilities.ToastUtils;
import org.odk.collect.android.widgets.GeoPointAggregator;
import org.odk.collect.android.widgets.GeoPointWidget;

import java.util.List;

public class GeoPointActivity extends AppCompatActivity implements LocationListener {

    // Instance state bundle
    private static final String LOCATION_COUNT = "locationCount";
    private static final String COLLECTED_POINTS = "collectedPoints";

    private AlertDialog dialog;
    private LocationManager locationManager;
    private boolean gpsOn = false;
    private boolean networkOn = false;

    private int locationProviderStatus = LocationProvider.TEMPORARILY_UNAVAILABLE;
    private double lastLocationAccuracy = 0;

    private double accuracyThreshold;
    private int locationCount = 0;
    private GeoPointAggregator points = new GeoPointAggregator();

    private TextView locationStatusView;
    private TextView collectionStatusView;
    private TextView accuracyThresholdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (savedInstanceState != null) {
            locationCount = savedInstanceState.getInt(LOCATION_COUNT);
            points = GeoPointAggregator.fromDoubleArray(savedInstanceState.getDoubleArray(COLLECTED_POINTS));
        }

        Intent intent = getIntent();

        accuracyThreshold = GeoPointWidget.DEFAULT_LOCATION_ACCURACY;
        if (intent != null && intent.getExtras() != null) {
            if (intent.hasExtra(GeoPointWidget.ACCURACY_THRESHOLD)) {
                accuracyThreshold = intent.getDoubleExtra(GeoPointWidget.ACCURACY_THRESHOLD,
                        GeoPointWidget.DEFAULT_LOCATION_ACCURACY);
            }
        }

        setTitle(getString(R.string.get_location));
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // make sure we have a good location provider before continuing
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
                gpsOn = true;
            }
            if (provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
                networkOn = true;
            }
        }
        if (!gpsOn && !networkOn) {
            ToastUtils.showShortToast(R.string.provider_disabled_error);
            Intent onGPSIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(onGPSIntent);
            finish();
        }

        if (gpsOn) {
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc != null) {
                InfoLogger.geolog("GeoPointActivity: " + System.currentTimeMillis()
                        + " lastKnownLocation(GPS) lat: "
                        + loc.getLatitude() + " long: "
                        + loc.getLongitude() + " acc: "
                        + loc.getAccuracy());
            } else {
                InfoLogger.geolog("GeoPointActivity: " + System.currentTimeMillis()
                        + " lastKnownLocation(GPS) null location");
            }
        }

        if (networkOn) {
            Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                InfoLogger.geolog("GeoPointActivity: " + System.currentTimeMillis()
                        + " lastKnownLocation(Network) lat: "
                        + loc.getLatitude() + " long: "
                        + loc.getLongitude() + " acc: "
                        + loc.getAccuracy());
            } else {
                InfoLogger.geolog("GeoPointActivity: " + System.currentTimeMillis()
                        + " lastKnownLocation(Network) null location");
            }
        }

        buildAndShowDialog();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LOCATION_COUNT, locationCount);
        outState.putDoubleArray(COLLECTED_POINTS, points.toDoubleArray());
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stops the GPS. Note that this will turn off the GPS if the screen goes to sleep.
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }

        // We're not using managed dialogs, so we have to dismiss the dialog to prevent it from
        // leaking memory.
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (locationManager != null) {
            if (gpsOn) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
            if (networkOn) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
        }
        if (dialog != null) {
            dialog.show();
            updateLabels();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Collect.getInstance().getActivityLogger().logOnStart(this);
    }

    @Override
    protected void onStop() {
        Collect.getInstance().getActivityLogger().logOnStop(this);
        super.onStop();
    }

    private void buildAndShowDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.geopoint_dialog_title));
        View view = getLayoutInflater().inflate(R.layout.geopoint_dialog, null);
        locationStatusView = (TextView) view.findViewById(R.id.geopoint_dialog_location_status);
        collectionStatusView = (TextView) view.findViewById(R.id.geopoint_dialog_collection_status);
        accuracyThresholdView = (TextView) view.findViewById(R.id.geopoint_dialog_accuracy_threshold);

        ((Button) view.findViewById(R.id.geopoint_decrement_accuracy_threshold)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                adjustAccuracyThreshold(-1);
            }
        });
        ((Button) view.findViewById(R.id.geopoint_increment_accuracy_threshold)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                adjustAccuracyThreshold(1);
            }
        });

        builder.setView(view)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Collect.getInstance().getActivityLogger().logInstanceAction(
                        this, "acceptLocation", "OK");
                    dialog.cancel();
                    finishWithLocationResult();
                }
            })
            .setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) { }  // replaced below
            })
            .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Collect.getInstance().getActivityLogger().logInstanceAction(
                        this, "cancelLocation", "cancel");
                    dialog.cancel();
                    finish();
                }
            })
            .setCancelable(false);  // back button doesn't cancel

        dialog = builder.create();
        dialog.show();

        // When the "Clear" button is pressed, we don't want to dismiss the
        // dialog, so we override the DialogInterface.OnClickListener with a
        // View.OnClickListener.  We have to wait until after the dialog is
        // shown to get our hands on the button and set its listener.
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                points = new GeoPointAggregator();  // reset to an empty aggregator
                updateLabels();
                // We don't dismiss the dialog.
            }
        });
    }

    private void finishWithLocationResult() {
        if (points.getNumAcceptablePoints(accuracyThreshold) > 0) {
            setResult(RESULT_OK, new Intent().putExtra(
                FormEntryActivity.LOCATION_RESULT,
                points.getCentroid(accuracyThreshold).getDisplayText()
            ));
        }
        finish();
    }

    private void adjustAccuracyThreshold(double delta) {
        accuracyThreshold += delta;
        updateLabels();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            InfoLogger.geolog("GeoPointActivity: " + System.currentTimeMillis()
                + " onLocationChanged(" + locationCount + ") null location");
            return;
        }

        // Bug report: cached GeoPoint is being returned as the first value.
        // Wait for the 2nd value to be returned, which is hopefully not cached?
        ++locationCount;
        InfoLogger.geolog("GeoPointActivity: " + System.currentTimeMillis()
            + " onLocationChanged(" + locationCount + ") " + location);

        if (locationCount > 1 && LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
            points.addLocation(location);
            locationProviderStatus = LocationProvider.AVAILABLE;
            lastLocationAccuracy = location.getAccuracy();
            updateLabels();
        }
    }

    /** Updates the status information in the dialog to reflect current state. */
    private void updateLabels() {
        accuracyThresholdView.setText(getString(
            R.string.geopoint_dialog_accuracy_threshold, accuracyThreshold));

        switch (locationProviderStatus) {
            case LocationProvider.OUT_OF_SERVICE:
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                locationStatusView.setText(R.string.geopoint_location_status_searching);
                break;
            case LocationProvider.AVAILABLE:
                if (lastLocationAccuracy > 0) {
                    locationStatusView.setText(getString(
                        lastLocationAccuracy < accuracyThreshold ?
                            R.string.geopoint_location_status_acceptable :
                            R.string.geopoint_location_status_unacceptable,
                        lastLocationAccuracy
                    ));
                }
                break;
        }

        boolean noneYet = (points.getNumPoints() == 0);
        int numPoints = points.getNumAcceptablePoints(accuracyThreshold);
        collectionStatusView.setText(
            noneYet ? getString(R.string.geopoint_collection_status_no_points_yet) :
            getResources().getQuantityString(
                R.plurals.geopoint_collection_status_points, numPoints, numPoints)
        );
    }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        locationProviderStatus = status;
        updateLabels();
    }
}
