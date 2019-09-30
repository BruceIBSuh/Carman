package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.text.InputType;

import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.threads.LoadDistCodeTask;
import com.silverback.carman2.views.SpinnerDialogPreference;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SettingPreferenceFragment extends PreferenceFragmentCompat {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingPreferenceFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private DecimalFormat df;
    //private SpinnerDialogPreference spinnerPref;
    private LoadDistCodeTask mTask;
    private String sidoName, sigunName, sigunCode;
    private String distCode;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Set Preference hierarchy defined as XML and placed in res/xml directory.
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Indicates that the fragment may initialize the contents of the Activity's standard options menu.
        setHasOptionsMenu(true);

        df = BaseActivity.getDecimalFormatInstance();

        // Retrvie the district info saved in SharedPreferences from the parent activity as a type
        // of JSONArray
        String[] district = getArguments().getStringArray("district");
        sigunCode = district[2];

        if (mDB == null) mDB = CarmanDatabase.getDatabaseInstance(getContext().getApplicationContext());

        // Custom SummaryProvider overriding provideSummary() with Lambda expression.
        // Otherwise, just set app:useSimpleSummaryProvider="true" in xml for EditTextPreference
        // and ListPreference.
        EditTextPreference etMileage = findPreference(Constants.ODOMETER);
        // Custom SummaryProvider overriding provideSummary() with Lambda expression.
        if (etMileage != null) {
            etMileage.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            etMileage.setSummaryProvider(preference -> String.format("%s%3s", etMileage.getText(), "km"));
        }

        EditTextPreference etAvg = findPreference(Constants.AVERAGE);
        //etAvg.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        if (etAvg != null) {
            etAvg.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            etAvg.setSummaryProvider(preference -> String.format("%s%3s", etAvg.getText(), "km"));
        }

        ListPreference searchingRadius = findPreference(Constants.RADIUS);
        if (searchingRadius != null) {
            searchingRadius.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }


        // Designate the gas station which indicates the price information in the main activity using
        // Room and LiveData.
        /*
        ListPreference desigStn = findPreference("pref_designated_station");
        desigStn.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

        mDB.favoriteModel().loadAllFavoriteProvider().observe(this, provider-> {
            List<String> stnList = new ArrayList<>();
            for(FavoriteProviderEntity entity : provider) {
                log.i("favorite list: %s, %s", entity.providerName, entity.address);
                stnList.add(entity.providerName);
            }

            String[] arrStn = new String[stnList.size()];
            for(int i = 0; i < stnList.size(); i++) arrStn[i] = stnList.get(i);
            desigStn.setEntries(arrStn);
            desigStn.setEntryValues(arrStn);

        });
        */

        SpinnerDialogPreference spinnerPref = findPreference(Constants.DISTRICT);
        spinnerPref.setSummary(String.format("%s %s", district[0], district[1]));

        SwitchPreferenceCompat switchPref = findPreference(Constants.LOCATION_UPDATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTask != null) mTask = null;
    }


    // Callback from PreferenceManager.OnDisplayPreferenceDialogListener when a Preference
    // requests to display a dialog.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onDisplayPreferenceDialog(Preference pref) {

        if (pref instanceof SpinnerDialogPreference) {
            DialogFragment dlgFragment = SettingSpinnerDlgFragment.newInstance(pref.getKey(), sigunCode);
            dlgFragment.setTargetFragment(this, 0);
            dlgFragment.show(getFragmentManager(), null);

        } else {
            super.onDisplayPreferenceDialog(pref);
        }

    }
}