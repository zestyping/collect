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

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.view.View;

import org.odk.collect.android.R;

import androidx.annotation.Nullable;

import static org.odk.collect.android.preferences.GeneralKeys.KEY_BASE_LAYER;
import static org.odk.collect.android.preferences.GeneralKeys.KEY_BASE_LAYER_TYPE;
import static org.odk.collect.android.preferences.PreferencesActivity.INTENT_KEY_ADMIN_MODE;

public class MapsPreferences extends BasePreferenceFragment {
    private ListPreference baseLayerTypePreference;

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

        Context context = getPreferenceScreen().getContext();
        baseLayerTypePreference = PreferenceUtils.createListPreference(
            context, KEY_BASE_LAYER_TYPE, R.string.base_layer_type,
            R.array.base_layer_type_entries, R.array.base_layer_type_values
        );
        populateBaseLayerOptions(baseLayerTypePreference.getValue());
        baseLayerTypePreference.setOnPreferenceChangeListener((pref, value) -> {
            populateBaseLayerOptions(value.toString());
            return true;
        });
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

    private void populateBaseLayerOptions(String value) {
        PreferenceCategory category = (PreferenceCategory) findPreference(KEY_BASE_LAYER);
        category.removeAll();
        category.addPreference(baseLayerTypePreference);
        PreferenceUtils.getBaseLayer(value).addPreferences(category);
    }

    private boolean failedLoadingMapPrefs(ListPreference mapSdk, ListPreference mapBasemap) {
        return mapSdk == null || mapBasemap == null
                || mapSdk.getEntryValues() == null || mapSdk.getEntries() == null
                || mapSdk.getEntryValues().length == 0 || mapSdk.getEntries().length == 0
                || mapBasemap.getEntryValues() == null || mapBasemap.getEntries() == null
                || mapBasemap.getEntryValues().length == 0 || mapBasemap.getEntries().length == 0;
    }
}
