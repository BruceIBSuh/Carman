package com.silverback.carman.views;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.json.JSONArray;

public class SpinnerDialogPreference extends DialogPreference {
    // Constructors
    public SpinnerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Setting the layout invokes onBindDialogView() of SettingSpinnerDlgFragment, which
        // binds views in the content view of the dialog to data. Then, onCreateDialog will be
        // invoked.
        setDialogLayoutResource(R.layout.dialog_setting_spinner);
    }
}