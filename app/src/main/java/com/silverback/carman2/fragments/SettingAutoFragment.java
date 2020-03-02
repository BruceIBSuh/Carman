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
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;


public class SettingAutoFragment extends PreferenceFragmentCompat implements PreferenceChangeListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);

    // Constants
    private static final int LONGEVITY = 20;

    // Objects
    private CarmanDatabase mDB;
    private SharedPreferences mSettings;
    private OnToolbarTitleListener mToolbarListener;
    private FragmentSharedModel fragmentSharedModel;
    private ListPreference autoMaker, autoType, autoModel, autoYear;
    private List<String> yearList;
    private List<String> autoModels;

    // fields
    private String[] yearEntries;



    // Interface for reverting the actionbar title. Otherwise, the title in the parent activity should
    // be reset to the current tile.
    public interface OnToolbarTitleListener {
        void notifyResetTitle();
    }

    // Set the listener to the parent activity for reverting the toolbar title.
    public void addTitleListener(OnToolbarTitleListener titleListener) {
        mToolbarListener = titleListener;
    }

    // Constructor
    public SettingAutoFragment() {}

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.pref_autodata, rootKey);
        setHasOptionsMenu(true);

        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentSharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        yearList = new ArrayList<>();

        //List<IntroActivity.AutoData> autoDataList = getAutoData();
        autoMaker = findPreference(Constants.AUTO_MAKER);
        autoType = findPreference(Constants.AUTO_TYPE);
        autoModel = findPreference(Constants.AUTO_MODEL);
        autoYear = findPreference(Constants.AUTO_YEAR);

        // Query the auto makers and set yearEntries(entryValues) to the autoMaker listpreference.
        List<String> autoMakers = mDB.autoDataModel().getAutoMaker();
        final int size = autoMakers.size();
        autoMaker.setEntries(autoMakers.toArray(new CharSequence[size]));
        autoMaker.setEntryValues(autoMakers.toArray(new CharSequence[size]));
        // Re-query auto models with a newly selected auto maker and set them to the autoModel
        // preference at the time that the auto model preference changes the value.
        autoMaker.setOnPreferenceChangeListener((preference, value)-> {
            // Re-query auto models each time
            //autoModel.setSummary(getString(R.string.pref_entry_void));
            autoModel.setSummary("다시 지정");
            setAutoModelEntries((String)value);
            return true;
        });

        // Set the initial entries and entryvalues
        String carMaker = mSettings.getString(Constants.AUTO_MAKER, null);
        autoModel.setSummary(Constants.AUTO_MODEL);
        if(carMaker != null) setAutoModelEntries(carMaker);
        /*
        // For the autoModel preference summary depends on which automaker users select in the
        // autoMaker PreferenceChangeListener, SummaryProvider is set to false.
        autoModel.setOnPreferenceChangeListener((preference, value) -> {
            //autoModel.setSummary((String)value);
            return true;
        });
        */

        String[] type = {"Sedan", "SUV", "MPV", "Mini Bus", "Truck", "Bus"};
        autoType.setEntries(type);
        autoType.setEntryValues(type);

        createYearEntries();
        autoYear.setEntries(yearEntries);
        autoYear.setEntryValues(yearEntries);

        log.i("autoMaker value: %s, %s", autoMaker.getEntry(), autoMaker.getValue());

    }

    // To make the Up button working in Fragment, it is required to invoke sethasOptionsMenu(true)
    // and the return value should be true in onOptionsItemSelected(). The values of each preference
    // is translated to List<String>, then converted to JSONString for transferring the json string
    // to SettingPerrenceFragment to invalidate the preference summary.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            // Invalidate the summary of the parent preference transferring the changed data to
            // as JSON string type.
            JSONArray autoData = new JSONArray(getAutoDataList());
            mSettings.edit().putString(Constants.AUTO_DATA, autoData.toString()).apply();
            fragmentSharedModel.getJsonAutoData().setValue(autoData.toString());

            // Revert the toolbar title when leaving this fragment b/c SettingPreferenceFragment and
            // SettingAutoFragment share the toolbar under the same parent activity.
            mToolbarListener.notifyResetTitle();

            return true;
        }

        return false;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {

    }

    private void createYearEntries() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        for(int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        yearEntries = yearList.toArray(new String[LONGEVITY]);
    }

    private List<String> getAutoDataList() {
        List<String> dataList = new ArrayList<>();

        dataList.add(autoMaker.getSummary().toString());
        dataList.add(autoType.getSummary().toString());
        dataList.add(autoModel.getSummary().toString());
        dataList.add(autoYear.getSummary().toString());

        return dataList;

    }

    private void setAutoModelEntries(String autoMaker) {
        List<String> autoModels = mDB.autoDataModel().queryAutoModels(autoMaker);
        final int modelSize = autoModels.size();
        autoModel.setEntries(autoModels.toArray(new CharSequence[modelSize]));
        autoModel.setEntryValues(autoModels.toArray(new CharSequence[modelSize]));
    }

    /*
    @SuppressWarnings("unchecked")
    private List<IntroActivity.AutoData> getAutoData() {

        try (FileInputStream fis = new FileInputStream(Constants.FILE_AUTO_DATA);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            ArrayList<IntroActivity.AutoData> dataSet = (ArrayList<IntroActivity.AutoData>)ois.readObject();
            log.i("dataSet: %s", dataSet.size());
            for (IntroActivity.AutoData data : dataSet) {
                log.i("Auto Maker: %s", data.getAutoMaker());
                log.i("Auto Model: %s", data.getAutoModel());
            }

            return dataSet;

        } catch(ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

     */

}
