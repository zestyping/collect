package org.odk.collect.android.utilities;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/** Created by Divya on 3/2/2017. */

public class PlayServicesUtil {
    private static final int PLAY_SERVICE_ERROR_REQUEST_CODE = 1000;
    private static int lastResultCode = ConnectionResult.SUCCESS;

    private PlayServicesUtil() { }  // prevent instantiation of this utility class

    /** Returns true if Google Play Services is installed and up to date. */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        lastResultCode = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context);
        return lastResultCode == ConnectionResult.SUCCESS;
    }

    /** Shows an error dialog for the last call to isGooglePlayServicesAvailable(). */
    public static void showGooglePlayServicesAvailabilityErrorDialog(Context context) {
        GoogleApiAvailability.getInstance().getErrorDialog(
            (Activity) context, lastResultCode, PLAY_SERVICE_ERROR_REQUEST_CODE).show();
    }
}
