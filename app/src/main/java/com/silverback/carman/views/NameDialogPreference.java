package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class NameDialogPreference extends DialogPreference implements Preference.OnPreferenceChangeListener{
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(NameDialogPreference.class);

    // Constructors
    public NameDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.dialog_setting_name);
        setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        log.i("onPreferenceChange");
        String nickname = String.valueOf(newValue);
        persistString(nickname);
        setSummary(nickname);
        return false;
    }
}
