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
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.map.BaseLayerType;
import org.odk.collect.android.map.BaseLayerTypeRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

import static org.odk.collect.android.preferences.GeneralKeys.CATEGORY_BASE_LAYER;
import static org.odk.collect.android.preferences.GeneralKeys.CATEGORY_REFERENCE_LAYER;
import static org.odk.collect.android.preferences.GeneralKeys.KEY_BASE_LAYER_TYPE;
import static org.odk.collect.android.preferences.GeneralKeys.KEY_REFERENCE_LAYER;
import static org.odk.collect.android.preferences.PreferencesActivity.INTENT_KEY_ADMIN_MODE;

public class MapsPreferences extends BasePreferenceFragment {
    private ListPreference mBaseLayerTypePref;
    private ListPreference mReferenceLayerPref;
    private Context mContext;

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

        mContext = getPreferenceScreen().getContext();
        initBaseLayerTypePref();
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

    /**
     * Creates the Base Layer Type preference (but doesn't add it to the screen;
     * onBaseLayerTypeChanged will do that part).
     */
    private void initBaseLayerTypePref() {
        mBaseLayerTypePref = PrefUtils.createListPref(
            mContext, KEY_BASE_LAYER_TYPE, R.string.base_layer_type,
            BaseLayerTypeRegistry.getNameResourceIds(), BaseLayerTypeRegistry.getIds()
        );
        onBaseLayerTypeChanged(null);
        mBaseLayerTypePref.setOnPreferenceChangeListener((pref, value) -> {
            onBaseLayerTypeChanged(value.toString());
            return true;
        });
    }

    /** Updates the rest of the preference UI when the Base Layer Type is changed. */
    private void onBaseLayerTypeChanged(String bltId) {
        BaseLayerType baseLayerType = bltId == null ?
            BaseLayerTypeRegistry.getCurrent(mContext) :
            BaseLayerTypeRegistry.get(bltId);
        baseLayerType.onSelected();

        PreferenceCategory baseCategory = getCategory(CATEGORY_BASE_LAYER);
        baseCategory.removeAll();
        baseCategory.addPreference(mBaseLayerTypePref);
        baseLayerType.addPreferences(baseCategory);

        PreferenceCategory referenceCategory = getCategory(CATEGORY_REFERENCE_LAYER);
        referenceCategory.removeAll();
        mReferenceLayerPref = createReferenceLayerPref(mContext, baseLayerType);
        referenceCategory.addPreference(mReferenceLayerPref);
    }

    /** Creates the Reference Layer preference for a given base layer type. */
    public static ListPreference createReferenceLayerPref(Context context, BaseLayerType baseLayerType) {
        List<File> files = getSupportedLayerFiles(baseLayerType);
        ListPreference pref = PrefUtils.createListPref(
            context, KEY_REFERENCE_LAYER, R.string.layer_data,
            toFilenameArray(files, context), toPathArray(files)
        );
        pref.setDialogTitle(
            context.getString(R.string.layer_data_dialog_title,
                Collect.OFFLINE_LAYERS,
                context.getString(baseLayerType.getNameResourceId())
            )
        );
        return pref;
    }

    private PreferenceCategory getCategory(String key) {
        return (PreferenceCategory) findPreference(key);
    }

    /** Gets the list of reference layer files supported by the current Base Layer Type. */
    private static List<File> getSupportedLayerFiles(BaseLayerType blt) {
        List<File> files = new ArrayList<>();
        files.add(null);  // the first option to show is always "None"; see null checks below
        for (File file : new File(Collect.OFFLINE_LAYERS).listFiles()) {
            if (blt.supportsLayer(file)) {
                files.add(file);
            }
        }
        return files;
    }

    private static String[] toFilenameArray(List<File> files, Context context) {
        String[] filenames = new String[files.size()];
        int i = 0;
        for (File file : files) {
            filenames[i++] = file == null ? context.getString(R.string.none) : file.getName();
        }
        return filenames;
    }

    private static String[] toPathArray(List<File> files) {
        String[] paths = new String[files.size()];
        int i = 0;
        for (File file : files) {
            paths[i++] = file == null ? "" : file.getAbsolutePath();
        }
        return paths;
    }
}
