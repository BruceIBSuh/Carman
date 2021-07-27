package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.json.JSONArray;

public class SpinnerDialogPreference extends DialogPreference implements Preference.OnPreferenceChangeListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerDialogPreference.class);


    /*
     * When you replace the 0 in the second constructor with R.attr.dialogPreferenceStyle
     * (for a DialogPreference) or R.attr.preferenceStyle (For any other preference) you
     * won’t face any design issues later. Thanks to Ivan Soriano
     */

    // Constructors
    public SpinnerDialogPreference(Context context) {
        super(context);
    }
    public SpinnerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public SpinnerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getAttributes(context, attrs);
    }
    public SpinnerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getAttributes(context, attrs);
    }

    private void getAttributes(Context context, AttributeSet attrs) {
        setDialogLayoutResource(R.layout.dialog_setting_spinner);
        setOnPreferenceChangeListener(this);
        setPositiveButtonText("CONFIRM");
        setNegativeButtonText("CANCEL");

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpinnerDialogPreference);
        try {

        } finally {
            typedArray.recycle();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        JSONArray json = (JSONArray)newValue;
        setSummary(String.format("%s %s", json.optString(0), json.optString(1)));

        return true;
    }

}