package com.silverback.carman2.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class SettingAutoFragment extends PreferenceFragmentCompat {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);
    // Constants
    private static final int LONGEVITY = 20;

    // Objects
    private SharedPreferences mSettings;
    private FragmentSharedModel fragmentSharedModel;
    private ListPreference autoMaker, autoType, autoModel, autoYear;
    private List<String> yearList;

    // fields
    private String[] entries;

    public SettingAutoFragment() {}

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.pref_autodata, rootKey);
        setHasOptionsMenu(true);

        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentSharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        yearList = new ArrayList<>();

        autoMaker = findPreference("pref_auto_maker");
        autoType = findPreference("pref_auto_type");
        autoModel = findPreference("pref_auto_model");
        autoYear = findPreference("pref_auto_year");

        String[] type = {"Sedan", "SUV", "Mini Bus", "Cargo", "Bus"};
        autoType.setEntries(type);
        autoType.setEntryValues(type);

        createYearEntries();
        autoYear.setEntries(entries);
        autoYear.setEntryValues(entries);

    }

    // To make the Up button working in Fragment, it is required to invoke sethasOptionsMenu(true)
    // and the return value should be true in onOptionsItemSelected(). The values of each preference
    // is translated to List<String>, then converted to JSONString for transferring the json string
    // to SettingPerrenceFragment to invalidate the preference summary.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        JSONArray autoData = new JSONArray(getAutoDataList());
        mSettings.edit().putString(Constants.VEHICLE, autoData.toString()).apply();
        fragmentSharedModel.getJsonAutoData().setValue(autoData.toString());

        return true;

    }

    private void createYearEntries() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for(int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        entries = yearList.toArray(new String[LONGEVITY]);
    }

    private  List<String> getAutoDataList() {
        List<String> dataList = new ArrayList<>();
        dataList.add(autoMaker.getSummary().toString());
        dataList.add(autoType.getSummary().toString());
        dataList.add(autoModel.getSummary().toString());
        dataList.add(autoYear.getSummary().toString());

        return dataList;

    }


}
