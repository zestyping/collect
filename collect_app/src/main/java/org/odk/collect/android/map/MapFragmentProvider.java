package org.odk.collect.android.map;

import android.content.Context;
import android.preference.PreferenceCategory;

import java.io.File;

import androidx.annotation.Nullable;

/**
 * Provides a MapFragment configured according to the user's preferences.
 * Each MapFragmentProvider can define its own preferences that customize the
 * base map.  For example, the GoogleMapFragmentProvider defines a "Google map
 * style" preference with Streets, Terrain, Hybrid, or Satellite as choices.
 */
public interface MapFragmentProvider {
    /**
     * Returns a stable unique ID for this base layer type.  This will be the
     * value saved to the preferences when this base layer type is selected.
     */
    String getId();

    /** Returns the resource ID for the user-facing name of this base layer type. */
    int getNameResourceId();

    /** Invoked when the user selects this provider in the preferences. */
    default void onSelected() { }

    /** Adds any preferences that are specific to this kind of MapFragment. */
    void addPreferences(PreferenceCategory category);

    /**
     * Creates a map fragment configured according to the preference settings.
     * This method can return null to indicate that there is no suitable
     * MapFragment implementation available.
     */
    @Nullable MapFragment createMapFragment(Context context);

    boolean supportsLayer(File file);
}
