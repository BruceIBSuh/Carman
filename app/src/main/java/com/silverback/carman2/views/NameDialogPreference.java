package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class NameDialogPreference extends DialogPreference implements Preference.OnPreferenceChangeListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(NameDialogPreference.class);

    // Constructors
    public NameDialogPreference(Context context) {
        super(context);
    }
    public NameDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public NameDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getAttributes(context, attrs);
    }
    public NameDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getAttributes(context, attrs);
    }


    private void getAttributes(Context context, AttributeSet attrs) {

        setDialogLayoutResource(R.layout.dialog_setting_edit_name);
        setOnPreferenceChangeListener(this);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpinnerDialogPreference);
        try {

        } finally {
            typedArray.recycle();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        log.i("onPreferenceChange");
        String strSummary = String.valueOf(newValue);
        setSummary(strSummary);

        return false;
    }
}
