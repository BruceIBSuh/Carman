package com.silverback.carman2.fragments;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class SettingAutoFragment extends PreferenceFragmentCompat {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);
    // Constants
    private static final int LONGEVITY = 20;

    // Objects
    private ListPreference autoMaker, autoType, autoModel, autoYear;
    private List<String> yearList;

    // fields
    private String[] entries;

    public SettingAutoFragment() {}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.pref_automaker_model, rootKey);
        setHasOptionsMenu(true);

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //getFragmentManager().popBackStack();
        getAutoDataList();
        return super.onOptionsItemSelected(item);

    }

    private void createYearEntries() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for(int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        entries = yearList.toArray(new String[LONGEVITY]);
    }

    public List<String> getAutoDataList() {
        List<String> dataList = new ArrayList<>();
        dataList.add(autoMaker.getSummary().toString());
        dataList.add(autoModel.getSummary().toString());
        dataList.add(autoYear.getSummary().toString());

        for(String str : dataList) log.i("Summaries: %s", str);
        return dataList;

    }


}
