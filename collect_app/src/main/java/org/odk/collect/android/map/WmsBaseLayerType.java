package org.odk.collect.android.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import org.odk.collect.android.preferences.PreferenceUtils;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;

public class WmsBaseLayerType implements BaseLayerType {
    private final String prefKey;
    private final int prefTitleId;
    private final WmsOption[] options;

    public WmsBaseLayerType(OnlineTileSourceBase source) {
        prefKey = "";
        prefTitleId = 0;
        options = new WmsOption[] {new WmsOption(0, "", source)};
    }

    public WmsBaseLayerType(String prefKey, int prefTitleId, WmsOption... options) {
        this.prefKey = prefKey;
        this.prefTitleId = prefTitleId;
        this.options = options;
    }

    @Override public void addPreferences(PreferenceCategory category) {
        if (options.length > 1) {
            int[] labelIds = new int[options.length];
            String[] values = new String[options.length];
            int i = 0;
            for (WmsOption option : options) {
                labelIds[i] = option.labelId;
                values[i] = option.value;
                i++;
            }
            category.addPreference(PreferenceUtils.createListPreference(
                category.getContext(), prefKey, prefTitleId, labelIds, values));
        }
    }

    @Override public MapFragment createMapFragment(Context context) {
        if (options.length > 1) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String value = prefs.getString(prefKey, null);
            for (int i = 0; i < options.length; i++) {
                if (options[i].value.equals(value)) {
                    return new OsmMapFragment(options[i].source);
                }
            }
            return null;
        } else {
            return new OsmMapFragment(options[0].source);
        }
    }

    public static class WmsOption {
        int labelId;
        String value;
        OnlineTileSourceBase source;

        public WmsOption(int labelId, String value, OnlineTileSourceBase source) {
            this.labelId = labelId;
            this.value = value;
            this.source = source;
        }
    }
}
