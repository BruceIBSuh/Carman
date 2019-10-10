package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderDao;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
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
    private SharedPreferences mSettings;

    private String sidoName, sigunName, sigunCode;
    private String distCode;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Set Preference hierarchy defined as XML and placed in res/xml directory.
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Indicates that the fragment may initialize the contents of the Activity's standard options menu.
        setHasOptionsMenu(true);
        if (mDB == null)
            mDB = CarmanDatabase.getDatabaseInstance(getContext().getApplicationContext());

        df = BaseActivity.getDecimalFormatInstance();
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();


        // Retrvie the district info saved in SharedPreferences from the parent activity as a type
        // of JSONArray
        String[] district = getArguments().getStringArray("district");
        sigunCode = district[2];

        // Custom SummaryProvider overriding provideSummary() with Lambda expression.
        // Otherwise, just set app:useSimpleSummaryProvider="true" in xml for EditTextPreference
        // and ListPreference.
        EditTextPreference etMileage = findPreference(Constants.ODOMETER);
        // Custom SummaryProvider overriding provideSummary() with Lambda expression.
        if(etMileage != null) {
            etMileage.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            etMileage.setSummaryProvider(preference -> String.format("%s%3s", etMileage.getText(), "km"));
        }

        EditTextPreference etAvg = findPreference(Constants.AVERAGE);
        //etAvg.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        if(etAvg != null) {
            etAvg.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            etAvg.setSummaryProvider(preference -> String.format("%s%3s", etAvg.getText(), "km"));
        }

        ListPreference searchingRadius = findPreference(Constants.RADIUS);
        if(searchingRadius != null) {
            searchingRadius.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }


        Preference favorite = findPreference("pref_favorite_provider");
        // Retrieve the favorite gas station and the service station which are both set the placeholder
        // to 0 as the designated provider.
        mDB.favoriteModel().queryFirstSetFavorite().observe(this, data -> {
            String station = getString(R.string.pref_no_favorite);
            String service = getString(R.string.pref_no_favorite);

            for(FavoriteProviderDao.FirstSetFavorite provider : data) {
                if(provider.category == Constants.GAS) station = provider.favoriteName;
                else if(provider.category == Constants.SVC) service = provider.favoriteName;
            }

            favorite.setSummary(String.format("%s / %s", station, service));
        });

        /*
        mDB.favoriteModel().queryFirstSetFavorite().observe(this, data -> {
            String station = getString(R.string.pref_no_favorite);
            String service = getString(R.string.pref_no_favorite);
            String providerId = null;

            for(FavoriteProviderDao.FirstSetFavorite provider : data) {
                if(provider.category == Constants.GAS){
                    station = provider.favoriteName;
                    providerId = provider.providerId;
                    log.i("ProviderId: %s", providerId);
                } else if(provider.category == Constants.SVC) {
                    service = provider.favoriteName;

                }

            }

            mSettings.edit().putString("pref_favorite_provider", providerId).apply();
            favorite.setSummary(String.format("%s / %s", station, service));
        });
        */

        Preference gasStation = findPreference("pref_favorite_gas");
        gasStation.setSummary(R.string.pref_summary_gas);

        Preference svcCenter = findPreference("pref_favorite_svc");
        svcCenter.setSummary(R.string.pref_summary_svc);

        ListPreference svcPeriod = findPreference("pref_service_period");
        if(svcPeriod != null) {
            svcPeriod.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        }

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