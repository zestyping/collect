package org.odk.collect.android.map;

import android.content.Context;
import android.preference.PreferenceCategory;

public class OsmBaseLayerType implements BaseLayerType {
    @Override public void addPreferences(PreferenceCategory category) { }

    @Override public MapFragment createMapFragment(Context context) {
        return new OsmMapFragment(
            org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
    }
}
