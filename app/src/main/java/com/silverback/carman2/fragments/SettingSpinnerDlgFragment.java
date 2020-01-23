package com.silverback.carman2.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.SpinnerDistrictModel;
import com.silverback.carman2.threads.LoadDistCodeTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.SpinnerDialogPreference;

import org.json.JSONArray;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * This class is a custom PreferenceDiglogFragmentCompat which contains the dual spinners, one of
 * which enlists the Sido name, the other enlists the Sigun names of the selected sido.
 *
 *
 */
public class SettingSpinnerDlgFragment extends PreferenceDialogFragmentCompat implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingSpinnerDlgFragment.class);

    // Objects
    private SpinnerDistrictModel distModel;
    private LoadDistCodeTask spinnerTask;
    private SpinnerDialogPreference spinnerPref;
    private Spinner sidoSpinner, sigunSpinner;
    private ArrayAdapter sidoAdapter;
    private DistrictSpinnerAdapter sigunAdapter;
    private SharedPreferences mSettings;
    private FragmentSharedModel fragmentSharedModel;

    // Fields
    private int mSidoItemPos, mSigunItemPos, tmpSidoPos, tmpSigunPos;

    public SettingSpinnerDlgFragment() {
        // Required empty public constructor
    }

    // Method for singleton instance
    static SettingSpinnerDlgFragment newInstance(String key, String code) {
        final SettingSpinnerDlgFragment fm = new SettingSpinnerDlgFragment();
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
        distModel = ViewModelProviders.of(this).get(SpinnerDistrictModel.class);
        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);

        String districtCode = getArguments().getString("distCode");
        String sidoCode = districtCode.substring(0, 2);
        String sigunCode = districtCode.substring(2,4);
        log.i("District Code: %s, %s", sidoCode, sigunCode);

        mSidoItemPos = Integer.valueOf(sidoCode) - 1;//Integer.valueOf("01") translates into 1
        mSigunItemPos = Integer.valueOf(sigunCode) - 1;// same as the aforementioned casting
        log.i("Item position: %s, %s", mSidoItemPos, mSigunItemPos);

        sidoAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.sido_name, android.R.layout.simple_spinner_item);
        sidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sidoSpinner.setAdapter(sidoAdapter);
        sidoSpinner.setSelection(mSidoItemPos, true);

        sigunAdapter = new DistrictSpinnerAdapter(getContext(), R.dimen.largeText);

        distModel.getSpinnerDataList().observe(this, dataList -> {
            if(sigunAdapter.getCount() > 0) sigunAdapter.removeAll();
            for(Opinet.DistrictCode obj : dataList) sigunAdapter.addItem(obj);

            sigunSpinner.setAdapter(sigunAdapter);
            sigunSpinner.setSelection(mSigunItemPos, true);
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
            log.i("Sido position: %s", position);
            // Initiate the task to retrieve sigun codes with the position of the Sigun spinner
            // which indicates the Sido code and the dataset returns via DistrictViewModel.
            // getSpinnerDataList()
            spinnerTask = ThreadManager.loadSpinnerDistCodeTask(getContext(), distModel, position);
            //tmpSidoPos = position;

            // Set the sigun spinner position to 0 only if mSidoItemPos changes.
            if(position != mSidoItemPos) {
                mSidoItemPos = position;
                mSigunItemPos = 0;
                log.i("mSidoItemPos: %s", mSidoItemPos);
            }

        } else if(parent == sigunSpinner) {
            log.i("SigunSpinner position: %s", position);
            //tmpSigunPos = position;
            mSigunItemPos = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        log.i("onNothingSelected");
    }

    // Should override onDialogClosed() defined in SpinnerDialogPreference to be invoked
    @Override
    public void onDialogClosed(boolean positiveResult) {

        if(positiveResult) {
            log.i("Save the code: %s, %s", mSidoItemPos, mSigunItemPos);
            //mSidoItemPos = tmpSidoPos;
            //mSigunItemPos = tmpSigunPos;

            //tmpSidoPos = 0;
            //tmpSigunPos = 0;
            //log.i("Save the code: %s, %s", mSidoItemPos, mSigunItemPos);

            String sidoName = (String)sidoAdapter.getItem(mSidoItemPos);
            String sigunName = sigunAdapter.getItem(mSigunItemPos).getDistrictName();
            String distCode = sigunAdapter.getItem(mSigunItemPos).getDistrictCode();
            log.i("District info: %s %s %s", sidoName, sigunName, distCode);

            // Share the district names with SettingPreferenceFragemnt to display the names in
            // the summary of the District preference.
            fragmentSharedModel.getDefaultDistNames().setValue(new String[]{sidoName, sigunName, distCode});
        }
    }

}
