package com.silverback.carman2.fragments;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.silverback.carman2.R;


public class SettingPrefAutoFragment extends PreferenceFragmentCompat {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.pref_automaker_model, rootKey);

        ListPreference maker = findPreference("pref_auto_maker");
        //ListPreference model = findPreference("pref_auto_model");
    }
}
