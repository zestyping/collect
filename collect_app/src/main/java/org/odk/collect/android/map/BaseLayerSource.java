package org.odk.collect.android.map;

import android.content.Context;
import android.preference.PreferenceCategory;

import java.io.File;

import androidx.annotation.Nullable;

/**
 * Instances of this interface represent "Source" options available in the
 * "Base Layer" preferences.  Each BaseLayerSource can define its own
 * preferences that customize the base layer, and its primary job is to
 * construct a MapFragment configured with an initial base layer according
 * to those preferences.  For example, the GoogleBaseLayerSource can define
 * a "Google map style" preference with choices such as Terrain or Satellite.
 */
public interface BaseLayerSource {
    /** Invoked when the user selects this provider in the Maps preferences. */
    void onSelected();

    /** Adds any preferences that are specific to this base layer source. */
    void addPrefs(PreferenceCategory category);

    /**
     * Creates a map fragment configured with a base layer according to the
     * preference settings.  This method can return null to indicate that
     * there is no suitable MapFragment implementation available.
     */
    @Nullable MapFragment createMapFragment(Context context);

    /**
     * Returns true if map fragments obtained from this BaseLayerSource are
     * expected to be able to render the given file as an overlay.  This
     * check determines which files appear as available Reference Layers.
     */
    boolean supportsLayer(File file);
}
