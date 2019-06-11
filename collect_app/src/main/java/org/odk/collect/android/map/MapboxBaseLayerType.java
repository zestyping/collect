package org.odk.collect.android.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.mapbox.mapboxsdk.maps.Style;

import org.odk.collect.android.R;
import org.odk.collect.android.preferences.PreferenceUtils;

import static org.odk.collect.android.preferences.GeneralKeys.KEY_MAPBOX_MAP_STYLE;

public class MapboxBaseLayerType implements BaseLayerType {
    @Override public void addPreferences(PreferenceCategory category) {
        category.addPreference(
            PreferenceUtils.createListPreference(
                category.getContext(),
                KEY_MAPBOX_MAP_STYLE,
                R.string.mapbox_map_style,
                new int[] {
                    R.string.mapbox_map_style_streets,
                    R.string.mapbox_map_style_light,
                    R.string.mapbox_map_style_dark,
                    R.string.mapbox_map_style_satellite,
                    R.string.mapbox_map_style_satellite_streets,
                    R.string.mapbox_map_style_outdoors,
                },
                new String[] {
                    Style.MAPBOX_STREETS,
                    Style.LIGHT,
                    Style.DARK,
                    Style.SATELLITE,
                    Style.SATELLITE_STREETS,
                    Style.OUTDOORS
                }
            )
        );
    }

    @Override public MapFragment createMapFragment(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String styleUrl = prefs.getString(KEY_MAPBOX_MAP_STYLE, Style.MAPBOX_STREETS);
        return new MapboxMapFragment(styleUrl);
    }
}
