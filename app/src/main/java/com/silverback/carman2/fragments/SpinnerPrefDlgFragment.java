package com.silverback.carman2.fragments;


import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.SpinnerDistCodeTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.views.SpinnerDialogPreference;

import org.json.JSONArray;

import java.util.Arrays;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * A simple {@link Fragment} subclass.
 */
public class SpinnerPrefDlgFragment extends PreferenceDialogFragmentCompat implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerPrefDlgFragment.class);

    // Objects
    private SpinnerDistCodeTask spinnerTask;
    private SpinnerDialogPreference spinnerPref;
    private Spinner sidoSpinner, sigunSpinner;
    private DistrictSpinnerAdapter sigunAdapter;

    // Fields
    private String sidoName, sigunName, distCode;



    public SpinnerPrefDlgFragment() {
        // Required empty public constructor
    }

    // Method for singleton instance
    static SpinnerPrefDlgFragment newInstance(String key) {

        log.i("SpinnerPrefDlgFragment: %s", key);
        final SpinnerPrefDlgFragment fm = new SpinnerPrefDlgFragment();
        final Bundle bundle = new Bundle(1);
        bundle.putString(ARG_KEY, key);
        fm.setArguments(bundle);
        return fm;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        String sidoCode = sharedPreferences.getString(Constants.DISTRICT, "0101").substring(0, 2);
        spinnerPref= (SpinnerDialogPreference) getPreference();

        // Sets sidoSpinner and sidoAdapter.
        sidoSpinner = view.findViewById(R.id.spinner_sido);
        sidoSpinner.setSelection(Integer.valueOf(sidoCode) - 1);//Integer.valueOf("01") translates into 1;
        sidoSpinner.setOnItemSelectedListener(this);
        ArrayAdapter sidoAdapter = spinnerPref.getSidoAdapter();
        sidoSpinner.setAdapter(sidoAdapter);

        // Sets sigunAdapter and sigunAdapter(DistrictSpinnerAdapter)
        sigunSpinner = view.findViewById(R.id.spinner_sigun);
        sigunSpinner.setOnItemSelectedListener(this);
        sigunAdapter = spinnerPref.getSigunAdapter();
        sigunSpinner.setAdapter(sigunAdapter);



    }

    @Override
    public void onPause() {
        super.onPause();
        if(spinnerTask != null) spinnerTask = null;
    }

    // Should override onDialogClosed() defined in SpinnerDialogPreference to be invoked
    @Override
    public void onDialogClosed(boolean positiveResult) {

        //spinnerPref.onDialogClosed(positiveResult);
        if(positiveResult) {
            String[] district = { sidoName, sigunName, distCode };
            JSONArray jsonArray = new JSONArray(Arrays.asList(district));
            spinnerPref.persistDistCode(jsonArray.toString());
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if(parent == sidoSpinner) {
            log.i("Spinner position: %s, %s", position, sidoSpinner.getItemAtPosition(position));
            spinnerTask = ThreadManager.startSpinnerDistCodeTask(spinnerPref, position);

            sidoName = (String)sidoSpinner.getItemAtPosition(position);

        } else {
            log.i("sigunSpinner: %s, %s", position, sigunSpinner.getItemAtPosition(position));
            sigunName = (String)sigunSpinner.getItemAtPosition(position);
            distCode = spinnerPref.getDistCodeList().get(position).getDistrictCode();

            log.i("Code and Name: %s, %s", sigunName, distCode);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
