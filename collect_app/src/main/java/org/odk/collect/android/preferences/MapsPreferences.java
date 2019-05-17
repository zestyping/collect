/*
 * Copyright (C) 2017 Shobhit
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.view.View;

import com.google.common.collect.ObjectArrays;

import org.odk.collect.android.R;
import org.odk.collect.android.spatial.MapHelper;

import androidx.annotation.Nullable;

import static android.content.Context.MODE_PRIVATE;
import static org.odk.collect.android.preferences.GeneralKeys.GOOGLE_MAPS_BASEMAP_DEFAULT;
import static org.odk.collect.android.preferences.GeneralKeys.GOOGLE_MAPS_BASEMAP_KEY;
import static org.odk.collect.android.preferences.GeneralKeys.KEY_MAP_BASEMAP;
import static org.odk.collect.android.preferences.GeneralKeys.KEY_MAP_SDK;
import static org.odk.collect.android.preferences.GeneralKeys.MAPBOX_MAPS_BASEMAP_DEFAULT;
import static org.odk.collect.android.preferences.GeneralKeys.MAPBOX_MAP_STYLE_KEY;
import static org.odk.collect.android.preferences.GeneralKeys.OSM_BASEMAP_KEY;
import static org.odk.collect.android.preferences.GeneralKeys.OSM_MAPS_BASEMAP_DEFAULT;
import static org.odk.collect.android.preferences.PreferencesActivity.INTENT_KEY_ADMIN_MODE;

public class MapsPreferences extends BasePreferenceFragment {

    public static MapsPreferences newInstance(boolean adminMode) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(INTENT_KEY_ADMIN_MODE, adminMode);

        MapsPreferences prefs = new MapsPreferences();
        prefs.setArguments(bundle);
        return prefs;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.maps_preferences);
        initMapPrefs();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar.setTitle(R.string.maps);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (toolbar != null) {
            toolbar.setTitle(R.string.general_preferences);
        }
    }

    private void initMapPrefs() {
        final ListPreference mapSdk = (ListPreference) findPreference(KEY_MAP_SDK);
        final ListPreference mapBasemap = (ListPreference) findPreference(KEY_MAP_BASEMAP);

        if (mapSdk == null || mapBasemap == null) {
            return;
        }

        if (mapSdk.getValue() == null || mapSdk.getEntry() == null) {
            mapSdk.setValueIndex(mapSdk.findIndexOfValue(GeneralKeys.DEFAULT_BASEMAP_KEY));
        }

        String[] onlineLayerEntryValues;
        String[] onlineLayerEntries;
        if (mapSdk.getValue().equals(OSM_BASEMAP_KEY)) {
            onlineLayerEntryValues = getResources().getStringArray(R.array.map_osm_basemap_selector_entry_values);
            onlineLayerEntries = getResources().getStringArray(R.array.map_osm_basemap_selector_entries);
        } else if (mapSdk.getValue().equals(GOOGLE_MAPS_BASEMAP_KEY)) {
            onlineLayerEntryValues = getResources().getStringArray(R.array.map_google_basemap_selector_entry_values);
            onlineLayerEntries = getResources().getStringArray(R.array.map_google_basemap_selector_entries);
        } else { // MAPBOX_BASEMAP_KEY
            onlineLayerEntryValues = getResources().getStringArray(R.array.map_mapbox_basemap_selector_entry_values);
            onlineLayerEntries = getResources().getStringArray(R.array.map_mapbox_basemap_selector_entries);
        }
        mapBasemap.setEntryValues(ObjectArrays.concat(onlineLayerEntryValues, MapHelper.getOfflineLayerListWithTags(), String.class));
        mapBasemap.setEntries(ObjectArrays.concat(onlineLayerEntries, MapHelper.getOfflineLayerListWithTags(), String.class));

        mapSdk.setSummary(mapSdk.getEntry());
        mapSdk.setOnPreferenceChangeListener((preference, newValue) -> {
            String[] onlineLayerEntryValues1;
            String[] onlineLayerEntries1;
            String value = (String) newValue;

            if (value.equals(GeneralKeys.OSM_BASEMAP_KEY)) {
                onlineLayerEntryValues1 = getResources().getStringArray(R.array.map_osm_basemap_selector_entry_values);
                onlineLayerEntries1 = getResources().getStringArray(R.array.map_osm_basemap_selector_entries);
                mapBasemap.setValue(OSM_MAPS_BASEMAP_DEFAULT);
            } else if (value.equals(GeneralKeys.GOOGLE_MAPS_BASEMAP_KEY)) {
                onlineLayerEntryValues1 = getResources().getStringArray(R.array.map_google_basemap_selector_entry_values);
                onlineLayerEntries1 = getResources().getStringArray(R.array.map_google_basemap_selector_entries);
                mapBasemap.setValue(GOOGLE_MAPS_BASEMAP_DEFAULT);
            } else {  // MAPBOX_BASEMAP_KEY
                onlineLayerEntryValues1 = getResources().getStringArray(R.array.map_mapbox_basemap_selector_entry_values);
                onlineLayerEntries1 = getResources().getStringArray(R.array.map_mapbox_basemap_selector_entries);
                mapBasemap.setValue(MAPBOX_MAPS_BASEMAP_DEFAULT);
            }
            mapBasemap.setEntryValues(ObjectArrays.concat(onlineLayerEntryValues1, MapHelper.getOfflineLayerListWithTags(), String.class));
            mapBasemap.setEntries(ObjectArrays.concat(onlineLayerEntries1, MapHelper.getOfflineLayerListWithTags(), String.class));
            mapBasemap.setSummary(mapBasemap.getEntry());

            mapSdk.setSummary(mapSdk.getEntries()[mapSdk.findIndexOfValue(value)]);
            return true;
        });

        CharSequence entry = mapBasemap.getEntry();
        if (entry != null) {
            mapBasemap.setSummary(entry);
        } else {
            mapBasemap.setSummary(mapBasemap.getEntries()[0]);
            mapBasemap.setValueIndex(0);
        }

        mapBasemap.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = ((ListPreference) preference).findIndexOfValue(newValue.toString());
            preference.setSummary(((ListPreference) preference).getEntries()[index]);
            SharedPreferences sharedPref = getActivity().getApplicationContext()
                .getSharedPreferences("mapboxStyle", MODE_PRIVATE);
            SharedPreferences.Editor edit = sharedPref.edit();
            edit.putString(MAPBOX_MAP_STYLE_KEY,
                ((ListPreference) preference).getEntries()[index].toString()).apply();
            return true;
        });
    }
}
