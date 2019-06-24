package org.odk.collect.android.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import org.odk.collect.android.preferences.PrefUtils;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;

import java.io.File;

import static org.odk.collect.android.preferences.GeneralKeys.KEY_REFERENCE_LAYER;

public class WmsBaseLayerType implements BaseLayerType {
    private final String bltId;
    private final int nameId;
    private final String prefKey;
    private final int prefTitleId;
    private final WmsOption[] options;

    /** Constructs a base layer that provides just one Web Map Service. */
    public WmsBaseLayerType(String bltId, int nameId, OnlineTileSourceBase source) {
        this.bltId = bltId;
        this.nameId = nameId;
        prefKey = "";
        prefTitleId = 0;
        options = new WmsOption[] {new WmsOption(0, "", source)};
    }

    /**
     * Constructs a base layer that provides a few Web Map Services to choose from.
     * The choice of which Web Map Service will be stored in a string preference.
     */
    public WmsBaseLayerType(
        String bltId, int nameId,
        String prefKey, int prefTitleId,
        WmsOption... options
    ) {
        this.bltId = bltId;
        this.nameId = nameId;
        this.prefKey = prefKey;
        this.prefTitleId = prefTitleId;
        this.options = options;
    }

    @Override public String getId() {
        return bltId;
    }

    @Override public int getNameResourceId() {
        return nameId;
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
            category.addPreference(PrefUtils.createListPref(
                category.getContext(), prefKey, prefTitleId, labelIds, values));
        }
    }

    @Override public MapFragment createMapFragment(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String referencePath = prefs.getString(KEY_REFERENCE_LAYER, null);
        File referenceLayer = referencePath == null ? null : new File(referencePath);

        if (options.length > 1) {
            String value = prefs.getString(prefKey, null);
            for (int i = 0; i < options.length; i++) {
                if (options[i].value.equals(value)) {
                    return new OsmMapFragment(options[i].source, referenceLayer);
                }
            }
        }
        return new OsmMapFragment(options[0].source, referenceLayer);
    }

    @Override public boolean supportsLayer(File path) {
        return false;
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
