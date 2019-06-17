package org.odk.collect.android.map;

import android.content.Context;
import android.preference.PreferenceCategory;

import androidx.annotation.Nullable;

/**
 * A grouping of base layers that are presented as a "Base layer type" on the
 * settings screen.  Each BaseLayerType may define additional preferences that
 * customize the base layer.  For example, one of the base layer types is
 * "Google", which defines a "Google map style" preference allowing the user
 * to select Streets, Terrain, Hybrid, or Satellite.
 */
public interface BaseLayerType {
    /** Invoked when the user selects this base layer type in the preferences. */
    default void onSelected() { }

    /** Adds any preferences that are specific to this type of base layer. */
    void addPreferences(PreferenceCategory category);

    /**
     * Creates a map fragment configured according to the preference settings
     * for this type of base layer.  This method can return null to indicate
     * that the base layer implementation is unsupported.
     */
    @Nullable MapFragment createMapFragment(Context context);
}
