package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class NameDialogPreference extends DialogPreference implements Preference.OnPreferenceChangeListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(NameDialogPreference.class);

    // Objects
    private EditText etName;

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
        setDialogLayoutResource(R.layout.dialog_setting_name);
        setOnPreferenceChangeListener(this);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.NameDialogPreference);
        try {

        } finally {
            ta.recycle();
        }
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        log.i("onPreferenceChange");
        String nickname = String.valueOf(newValue);
        persistString(nickname);
        setSummary(nickname);
        return false;
    }

    public void setTitleColor(int color) {

    }
}
