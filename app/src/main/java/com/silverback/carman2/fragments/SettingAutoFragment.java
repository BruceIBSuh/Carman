package com.silverback.carman2.fragments;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.IntroActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class SettingAutoFragment extends PreferenceFragmentCompat {

    private static final LoggingHelper log = LoggingHelperFactory.create(SettingAutoFragment.class);
    // Constants
    private static final int LONGEVITY = 20;

    // Objects
    private SharedPreferences mSettings;
    private OnToolbarTitleListener mToolbarListener;
    private FragmentSharedModel fragmentSharedModel;
    private ListPreference autoMaker, autoType, autoModel, autoYear;
    private List<String> yearList;

    // fields
    private String[] entries;

    // Interface for reverting the actionbar title
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

        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        fragmentSharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        yearList = new ArrayList<>();

        //List<IntroActivity.AutoData> autoDataList = getAutoData();

        autoMaker = findPreference(Constants.AUTO_MAKER);
        autoType = findPreference(Constants.AUTO_TYPE);
        autoModel = findPreference(Constants.AUTO_MODEL);
        autoYear = findPreference(Constants.AUTO_YEAR);

        String[] type = { getString(R.string.pref_entry_void),"Sedan", "SUV", "Mini Bus", "Cargo", "Bus"};
        autoType.setEntries(type);
        autoType.setEntryValues(type);

        createYearEntries();
        autoYear.setEntries(entries);
        autoYear.setEntryValues(entries);

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

    private void createYearEntries() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        yearList.add(0, "설정안함");
        for(int i = year; i >= (year - LONGEVITY); i--) yearList.add(String.valueOf(i));
        entries = yearList.toArray(new String[LONGEVITY]);
    }

    private List<String> getAutoDataList() {
        List<String> dataList = new ArrayList<>();

        dataList.add(autoMaker.getSummary().toString());
        dataList.add(autoType.getSummary().toString());
        dataList.add(autoModel.getSummary().toString());
        dataList.add(autoYear.getSummary().toString());

        return dataList;

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
