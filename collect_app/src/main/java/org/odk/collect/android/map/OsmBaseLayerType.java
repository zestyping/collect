package org.odk.collect.android.map;

import android.content.Context;
import android.preference.PreferenceCategory;

import org.odk.collect.android.map.MbtilesFile.LayerType;

import java.io.File;

import timber.log.Timber;

public class OsmBaseLayerType implements BaseLayerType {
    @Override public void addPreferences(PreferenceCategory category) { }

    @Override public MapFragment createMapFragment(Context context) {
        return new OsmMapFragment(
            org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
    }

    @Override public boolean supportsLayer(File file) {
        if (file.getName().endsWith(".mbtiles")) {
            try {
                // GoogleMapFragment supports only raster tiles.
                return new MbtilesFile(file).getLayerType() == LayerType.RASTER;
            } catch (MbtilesFile.MbtilesException e) {
                Timber.d(e);
            }
        }
        return false;
    }
}
