package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.LoadDistCodeTask;
import com.silverback.carman2.views.SpinnerDialogPreference;

import java.text.DecimalFormat;

/**
 * A simple {@link PreferenceFragmentCompat} subclass.
 */
public class GeneralSettingFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralSettingFragment.class);

    // Objects
    private DecimalFormat df;
    private SpinnerDialogPreference spinnerPref;
    private LoadDistCodeTask mTask;
    private String sidoName, sigunName, sigunCode;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);

        df = BaseActivity.getDecimalFormatInstance();

        // Retrvie the district info saved in SharedPreferences from the parent activity as a type
        // of JSONArray
        String[] district = getArguments().getStringArray("district");
        sigunCode = district[2];

        // Custom SummaryProvider with Lambda expression for making numbers decimal-formatted.
        // Otherwise, just set app:useSimpleSummaryProvider="true" in xml for EditTextPreference
        // and ListPreference.
        EditTextPreference etMileage = findPreference(Constants.ODOMETER);
        etMileage.setSummaryProvider((Preference preference) -> {
            int mileage = Integer.valueOf(((EditTextPreference)preference).getText());
            return String.format("%s%5s", df.format(mileage), "km");
        });

        EditTextPreference etAvg = findPreference(Constants.AVERAGE);
        etAvg.setSummaryProvider((Preference preference) -> {
                int avg = Integer.valueOf(((EditTextPreference)preference).getText());
                return String.format("%s%5s",df.format(avg), "km");
        });


        SpinnerDialogPreference spinnerPref = findPreference("pref_dialog_district");
        spinnerPref.setSummary(String.format("%s %s", district[0], district[1]));

        SwitchPreferenceCompat switchPref = findPreference("pref_location_autoupdate");

    }

    @Override
    public void onPause() {
        super.onPause();
        if(mTask != null) mTask = null;
    }


    // Interface definition of PreferenceManager.OnDisplayPreferenceDialogListener when a Preference
    // requests to display a dialog.
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

    // Interface definition of a callback(Preference.OnPreferenceClickListener) to be invoked
    // when a Preference is clicked.
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
