package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.SpinnerDistrictModel;
import com.silverback.carman2.threads.LoadDistCodeTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.views.SpinnerDialogPreference;

import org.json.JSONArray;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * A simple {@link Fragment} subclass.
 */
public class SpinnerPrefDlgFragment extends PreferenceDialogFragmentCompat implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerPrefDlgFragment.class);

    // Objects
    private SpinnerDistrictModel distModel;
    private LoadDistCodeTask spinnerTask;
    private SpinnerDialogPreference spinnerPref;
    private Spinner sidoSpinner, sigunSpinner;
    private ArrayAdapter sidoAdapter;
    private DistrictSpinnerAdapter sigunAdapter;
    private SharedPreferences mSettings;
    // Fields
    private String sidoName, sigunName, distCode;
    private int mSidoItemPos, mSigunItemPos, tmpSidoPos, tmpSigunPos;



    public SpinnerPrefDlgFragment() {
        // Required empty public constructor
    }

    // Method for singleton instance
    static SpinnerPrefDlgFragment newInstance(String key, String code) {

        final SpinnerPrefDlgFragment fm = new SpinnerPrefDlgFragment();

        final Bundle args = new Bundle(2);
        args.putString(ARG_KEY, key);
        args.putString("distCode", code);
        fm.setArguments(args);

        return fm;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        sidoSpinner = view.findViewById(R.id.spinner_sido);
        sigunSpinner = view.findViewById(R.id.spinner_sigun);
        sidoSpinner.setOnItemSelectedListener(this);
        sigunSpinner.setOnItemSelectedListener(this);

        mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
        spinnerPref= (SpinnerDialogPreference)getPreference();

        String districtCode = getArguments().getString("distCode");
        String sidoCode = districtCode.substring(0, 2);
        String sigunCode = districtCode.substring(2,4);
        log.i("District Code: %s, %s", sidoCode, sigunCode);

        mSidoItemPos = Integer.valueOf(sidoCode) - 1; //Integer.valueOf("01") translates into 1
        mSigunItemPos = Integer.valueOf(sigunCode) - 1;
        log.i("Item position: %s, %s", mSidoItemPos, mSigunItemPos);

        // Sets sidoSpinner and sidoAdapter.
        sidoAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.sido_name, android.R.layout.simple_spinner_item);
        sidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sidoSpinner.setAdapter(sidoAdapter);
        sidoSpinner.setSelection(mSidoItemPos);


        sigunAdapter = new DistrictSpinnerAdapter(getContext());
        distModel = ViewModelProviders.of(this).get(SpinnerDistrictModel.class);
        distModel.getSpinnerDataList().observe(this, dataList -> {
            log.i("DataList: %s", dataList.size());
            if(sigunAdapter.getCount() > 0) sigunAdapter.removeAll();
            for(Opinet.DistrictCode obj : dataList) {
                sigunAdapter.addItem(obj);
            }
            sigunSpinner.setAdapter(sigunAdapter);
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        if(spinnerTask != null) spinnerTask = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        log.i("onItemSelected: %s", position);
        if(parent == sidoSpinner) {
            spinnerTask = ThreadManager.loadSpinnerDistCodeTask(getContext(), distModel, position);
            tmpSidoPos = position;

            // Set sigunSpinner positin to 0 only if mSidoItemPos changes.
            if(position != mSidoItemPos) mSigunItemPos = 0;

        } else {
            tmpSigunPos = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // Should override onDialogClosed() defined in SpinnerDialogPreference to be invoked
    @Override
    public void onDialogClosed(boolean positiveResult) {

        if(positiveResult) {

            mSidoItemPos = tmpSidoPos;
            mSigunItemPos = tmpSigunPos;

            String sidoName = (String)sidoAdapter.getItem(mSidoItemPos);
            String sigunName = sigunAdapter.getItem(mSigunItemPos).getDistrictName();
            String distCode = sigunAdapter.getItem(mSigunItemPos).getDistrictCode();
            log.i("District info: %s %s %s", sidoName, sigunName, distCode);

            JSONArray jsonArray = new JSONArray(Arrays.asList(sidoName, sigunName, distCode));
            spinnerPref.callChangeListener(jsonArray);

            // Save values in SharedPreferences
            mSettings.edit().putString(Constants.DISTRICT, jsonArray.toString()).apply();
        }
    }


    public DistrictSpinnerAdapter getSigunAdapter() {
        return sigunAdapter;
    }

    // Callback invoked from ThreadManager when the task has been completed.
    public void onDistrictTaskComplete() {
        sigunSpinner.setAdapter(sigunAdapter);
        sigunSpinner.setSelection(mSigunItemPos);
    }
}
