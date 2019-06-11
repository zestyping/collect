package org.odk.collect.android.map;

import android.content.Context;
import android.preference.PreferenceCategory;

/**
 * A grouping of base layers that are presented as a "Base layer type" on the
 * settings screen.  Each BaseLayerType may define additional preferences that
 * customize the base layer.  For example, one of the base layer types is
 * "Google", which defines a "Google map style" preference allowing the user
 * to select Streets, Terrain, Hybrid, or Satellite.
 */
public interface BaseLayerType {
    /** Adds any preferences that are specific to this type of base layer. */
    void addPreferences(PreferenceCategory category);

    /** Creates a map fragment appropriate to the preferences for this type of base layer. */
    MapFragment createMapFragment(Context context);
}
