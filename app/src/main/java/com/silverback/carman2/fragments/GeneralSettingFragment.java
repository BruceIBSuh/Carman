package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.LoadDistCodeTask;
import com.silverback.carman2.views.SpinnerDialogPreference;

import org.json.JSONArray;
import org.json.JSONException;

import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * A simple {@link PreferenceFragmentCompat} subclass.
 */
public class GeneralSettingFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralSettingFragment.class);

    // Objects
    private SharedPreferences sharedPreferences;
    private SpinnerDialogPreference spinnerPref;
    private LoadDistCodeTask mTask;
    private String sidoName, sigunName, sigunCode;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Retrvie the district info saved in SharedPreferences from the parent activity as a type
        // of JSONArray
        String[] district = getArguments().getStringArray("district");
        sigunCode = district[2];
        log.i("sigun code in GeneralSettingFragment: %s", sigunCode);

        EditTextPreference etPref = (EditTextPreference)findPreference("pref_nickname");
        etPref.setSummary(getArguments().getString("name"));


        SpinnerDialogPreference spinnerPref = (SpinnerDialogPreference)findPreference("pref_dialog_district");
        spinnerPref.setSummary(String.format("%s %s", district[0], district[1]));


    }

    @Override
    public void onPause() {
        super.onPause();
        if(mTask != null) mTask = null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onDisplayPreferenceDialog(Preference pref) {

        if(pref instanceof SpinnerDialogPreference) {
            DialogFragment dlgFragment = SpinnerPrefDlgFragment.newInstance(pref.getKey(), sigunCode);
            dlgFragment.setTargetFragment(this, 0);
            dlgFragment.show(getFragmentManager(), "spinner");

        } else {
            super.onDisplayPreferenceDialog(pref);
        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        log.i("onPreferenceClick");

        switch(preference.getKey()) {
            case "pref_fuel":
                log.i("Pref for Fuel: %s", preference.getKey());
                break;
        }

        return true;
    }
}
