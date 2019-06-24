package org.odk.collect.android.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.ListPreference;
import android.preference.PreferenceManager;

import java.util.Arrays;

public class PrefUtils {
    private PrefUtils() { }  // prevent instantiation of this utility class

    public static SharedPreferences getSharedPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static ListPreference createListPref(
        Context context, String key, int titleId, int labelsId, int valuesId) {
        Resources resources = context.getResources();
        return createListPref(context, key, titleId,
            resources.getStringArray(labelsId), resources.getStringArray(valuesId));
    }

    public static ListPreference createListPref(
        Context context, String key, int titleId, int[] labelIds, String[] values) {
        Resources resources = context.getResources();
        String[] labels = new String[labelIds.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = resources.getString(labelIds[i]);
        }
        return createListPref(context, key, titleId, labels, values);
    }

    public static ListPreference createListPref(
        Context context, String key, int titleId, String[] labels, String[] values) {
        ensurePrefHasValidValue(context, key, values);
        ListPreference pref = new ListPreference(context);
        pref.setKey(key);
        pref.setPersistent(true);
        pref.setTitle(titleId);
        pref.setDialogTitle(titleId);
        pref.setEntries(labels);
        pref.setEntryValues(values);
        pref.setSummary("%s");
        return pref;
    }

    public static void ensurePrefHasValidValue(Context context, String key, String[] values) {
        SharedPreferences prefs = getSharedPrefs(context);
        String value = prefs.getString(key, null);
        if (Arrays.asList(values).indexOf(value) < 0) {
            if (values.length > 0) {
                prefs.edit().putString(key, values[0]).apply();
            } else {
                prefs.edit().remove(key).apply();
            }
        }
    }

    /** Gets the BaseLayerType object for a given base_layer_type preference value. */
    /*

    public static BaseLayerType getBaseLayerType(String value) {
        BaseLayerType result = BASE_LAYER_TYPE_MAP.get(value);
        return result == null ? DEFAULT_BASE_LAYER_TYPE : result;
    }

    private static Map<String, BaseLayerType> getBaseLayerTypeMap() {
        TileSourceFactory factory = new TileSourceFactory();
        Map<String, BaseLayerType> map = new HashMap<>();
        map.put(GeneralKeys.BASE_LAYER_TYPE_GOOGLE, new GoogleBaseLayerType());
        map.put(GeneralKeys.BASE_LAYER_TYPE_MAPBOX, new MapboxBaseLayerType());
        map.put(GeneralKeys.BASE_LAYER_TYPE_OSM, new WmsBaseLayerType(TileSourceFactory.MAPNIK));
        map.put(GeneralKeys.BASE_LAYER_TYPE_USGS, new WmsBaseLayerType(
            GeneralKeys.KEY_USGS_MAP_STYLE, R.string.usgs_map_style,
            new WmsOption(R.string.usgs_map_style_topo, "topo", factory.getUSGSTopo()),
            new WmsOption(R.string.usgs_map_style_hybrid, "hybrid", factory.getUsgsSat()),
            new WmsOption(R.string.usgs_map_style_imagery, "imagery", factory.getUsgsImg())
        ));
        map.put(GeneralKeys.BASE_LAYER_TYPE_STAMEN, new WmsBaseLayerType(factory.getStamenTerrain()));
        map.put(GeneralKeys.BASE_LAYER_TYPE_CARTO, new WmsBaseLayerType(
            GeneralKeys.KEY_CARTO_MAP_STYLE, R.string.carto_map_style,
            new WmsOption(R.string.carto_map_style_positron, "positron", factory.getCartoDbPositron()),
            new WmsOption(R.string.carto_map_style_dark_matter, "dark_matter", factory.getCartoDbDarkMatter())
        ));
        return map;
    }
    */
}
