package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.SpinnerDistCodeTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.views.SpinnerDialogPreference;

import androidx.fragment.app.DialogFragment;
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
    private SpinnerDistCodeTask mTask;
    private String districtCode;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);
        districtCode = getArguments().getString("districtCode");
        log.i("District Code in PreferenceFragmentCompat: %s", districtCode);

    }

    @Override
    public void onResume(){
        super.onResume();
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
            //String code = sharedPreferences.getString(Constants.DISTRICT, "");
            //log.i("District Code: %s", code);

            DialogFragment dlgFragment = SpinnerPrefDlgFragment.newInstance(pref.getKey(), districtCode);
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
