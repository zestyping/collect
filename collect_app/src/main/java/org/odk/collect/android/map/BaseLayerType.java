package org.odk.collect.android.map;

import android.content.Context;
import android.preference.PreferenceCategory;

import java.io.File;

import androidx.annotation.Nullable;

/**
 * A "base layer type" is a set of base layer choices that share the same
 * underlying implementation and are given a single name on the preferences
 * screen.  Each BaseLayerType can define additional preferences that customize
 * the base layer.  For example, one of the base layer types is "Google", which
 * defines a "Google map style" preference allowing the user to select Streets,
 * Terrain, Hybrid, or Satellite.
 */
public interface BaseLayerType {
    /**
     * Returns a stable unique ID for this base layer type.  This will be the
     * value saved to the preferences when this base layer type is selected.
     */
    String getId();

    /** Returns the resource ID for the user-facing name of this base layer type. */
    int getNameResourceId();

    /** Invoked when the user selects this base layer type in the preferences. */
    default void onSelected() { }

    /** Adds any preferences that are specific to this type of base layer. */
    void addPreferences(PreferenceCategory category);

    /**
     * Creates a map fragment configured according to the preference settings
     * for this type of base layer.  This method can return null to indicate
     * that the base layer implementation is unavailable on the current device.
     */
    @Nullable MapFragment createMapFragment(Context context);

    boolean supportsLayer(File file);
}
