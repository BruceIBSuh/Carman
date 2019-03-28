package com.silverback.carman2.fragments;


import android.os.Bundle;

import com.silverback.carman2.R;
import com.silverback.carman2.views.DistrictDialogPreference;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * A simple {@link PreferenceFragmentCompat} subclass.
 */
public class GeneralSettingFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceClickListener {

    // Objects

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);

        DistrictDialogPreference dialog = (DistrictDialogPreference)findPreference("pref_dialog_district");
        dialog.setDialogLayoutResource(R.layout.spinners_district);
        dialog.setOnPreferenceClickListener(this);



    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        return true;
    }
}
