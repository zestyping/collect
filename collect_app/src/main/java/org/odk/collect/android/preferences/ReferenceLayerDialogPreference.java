package org.odk.collect.android.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.map.MapConfigurator;

import java.io.File;
import java.util.List;

public class ReferenceLayerDialogPreference extends DialogPreference {
    private final Context context;
    private int labelId;
    private List<File> files;
    private RadioGroup radioGroup;
    private TextView caption;

    public ReferenceLayerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setDialogLayoutResource(R.layout.reference_layer_dialog);
    }

    public void setOption(MapConfigurator.Option option) {
        labelId = option.labelId;
        updateContent();
    }

    public void setFiles(List<File> files) {
        this.files = files;
        updateContent();
    }

    protected void updateContent() {
        if (radioGroup != null) {
            radioGroup.removeAllViews();
            radioGroup.addView(inflateRadioButton(context.getString(R.string.none), -1));
            for (int i = 0; i < files.size(); i++) {
                radioGroup.addView(inflateRadioButton(files.get(i).getName(), i));
            }
        }
        if (caption != null) {
            String baseLabel = context.getString(labelId);
            caption.setText(context.getString(
                files.isEmpty() ? R.string.layer_data_caption_none : R.string.layer_data_caption,
                Collect.OFFLINE_LAYERS, baseLabel
            ));
        }
    }

    protected RadioButton inflateRadioButton(String text, int i) {
        RadioButton button = (RadioButton) LayoutInflater.from(context).inflate(R.layout.reference_layer_item, null);
        button.setText(text);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (i >= 0 && i < files.size()) {
                    setSummary(files.get(i).getAbsolutePath());
                }
            }
        });
        return button;
    }

    @Override protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        radioGroup = view.findViewById(R.id.radio_group);
        caption = view.findViewById(R.id.caption);
        updateContent();
    }
}
