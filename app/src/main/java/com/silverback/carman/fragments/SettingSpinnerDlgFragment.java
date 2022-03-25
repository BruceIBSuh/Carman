package com.silverback.carman.fragments;


import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.firebase.firestore.util.Assert;
import com.silverback.carman.R;
import com.silverback.carman.adapters.SigunSpinnerAdapter;
import com.silverback.carman.databinding.DialogSettingSpinnerBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.DistCodeSpinnerTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.views.SpinnerDialogPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * This class is a custom PreferenceDiglogFragmentCompat which contains the dual spinners. The Sido
 * spinner enlists each Sido names provided from the resource. The item position is identical with
 * the Sido code other than from the position 11 on, which indicates Daegu and more sequentially.
 * The Sigun spinner, however, is not guaranteed to have the position determined by its Sigun codes.
 * The code should be retrieved using DitrictCodeTask, worker thread having the Sido code as params.
 */
public class SettingSpinnerDlgFragment extends PreferenceDialogFragmentCompat implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingSpinnerDlgFragment.class);

    // Objects
    private SpinnerDialogPreference spinnerDialog;
    private DialogSettingSpinnerBinding binding;
    private OpinetViewModel opinetModel;
    private DistCodeSpinnerTask spinnerTask;
    //private Spinner sidoSpinner, sigunSpinner;
    private ArrayAdapter<CharSequence> sidoAdapter;
    private SigunSpinnerAdapter sigunAdapter;
    private FragmentSharedModel fragmentModel;

    // Fields
    private String districtCode;
    private int mSidoItemPos, mSigunItemPos, tmpSidoPos, tmpSigunPos;

    private SettingSpinnerDlgFragment() {
        // Required empty public constructor
    }

    // Singleton consructor
    static SettingSpinnerDlgFragment newInstance(String key, String code) {
        final SettingSpinnerDlgFragment fm = new SettingSpinnerDlgFragment();
        final Bundle args = new Bundle(2);
        args.putString(ARG_KEY, key);
        args.putString("distCode", code);
        fm.setArguments(args);

        return fm;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        binding = DialogSettingSpinnerBinding.inflate(LayoutInflater.from(getContext()));
        binding.spinnerSido.setOnItemSelectedListener(this);
        binding.spinnerSigun.setOnItemSelectedListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot())
                .setTitle(R.string.pref_dialog_district_title)
                .setPositiveButton("Confirm", this)
                .setNegativeButton("Cancel", this);
        AlertDialog dialog = builder.create();
        dialog.show();

        String districtCode = requireArguments().getString("distCode");
        // Integer.valueOf("01") fortunately translates into 1^^.
        if(districtCode != null){
            int sidoCode = Integer.parseInt(districtCode.substring(0, 2));
            mSidoItemPos = (sidoCode < 14) ? sidoCode - 1 : sidoCode - 3;
        }

        sidoAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sido_name, R.layout.spinner_settings_entry);
        sidoAdapter.setDropDownViewResource(R.layout.spinner_settings_dropdown);
        binding.spinnerSido.setAdapter(sidoAdapter);
        binding.spinnerSido.setSelection(mSidoItemPos, true);
        sigunAdapter = new SigunSpinnerAdapter(getContext());

        return dialog;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        //sidoSpinner = view.findViewById(R.id.spinner_sido);
        //sigunSpinner = view.findViewById(R.id.spinner_sigun);
        //sidoSpinner.setOnItemSelectedListener(this);
        //sigunSpinner.setOnItemSelectedListener(this);

        // The integer Sido code is not always the same as the position in the spinner in terms not
        // only of the spinner position starting with 0, which is different from the Sido code starting
        // with "01", but also of the Sido code, the number of which is not sequentially numbered from
        // the city of Daegu on. This city is positioned at 11 in the spinner but the code is numbered
        // as 14.
//
//        String districtCode = getArguments().getString("distCode");
//        // Integer.valueOf("01") fortunately translates into 1^^.
//        int sidoCode = Integer.parseInt(districtCode.substring(0, 2));
//        mSidoItemPos = (sidoCode < 14) ? sidoCode - 1 : sidoCode - 3;

        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);
        fragmentModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);

//        sidoAdapter = ArrayAdapter.createFromResource(getContext(), R.array.sido_name, R.layout.spinner_settings_entry);
//        sidoAdapter.setDropDownViewResource(R.layout.spinner_settings_dropdown);
//        sidoSpinner.setAdapter(sidoAdapter);
//        sidoSpinner.setSelection(mSidoItemPos, true);
//        sigunAdapter = new SigunSpinnerAdapter(getContext());

        // A Sigun list is notified as DistcodeSpinnerTask completes via SpinnerDistrictModel.
        opinetModel.getSpinnerDataList().observe(this, sigunList -> {
            log.i("opinetModel: %s", sigunList);
            if(sigunAdapter.getCount() > 0) sigunAdapter.removeAll();
            // Add the Sigun dataset received from DistrictCodeTask by SpinnerDistrictMode.
            sigunAdapter.addSigunList(sigunList);
            // Get the position of the Sigun spinner by comparing the default Sigun code with each
            // Sigun codes downloaded from the Opinet.
            if(mSidoItemPos != tmpSidoPos) mSigunItemPos = 0;
            else {
                int position = 0;
                for(Opinet.DistrictCode code :  sigunList) {
                    if (code.getDistrictCode().equals(districtCode)) mSigunItemPos = position;
                    position++;
                }
            }

            binding.spinnerSigun.setAdapter(sigunAdapter);
            binding.spinnerSigun.setSelection(mSigunItemPos, true);
        });
    }



    @Override
    public void onPause() {
        super.onPause();
        if(spinnerTask != null) spinnerTask = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // If the Sido spinner is selected as is the default Sido code is transferred
        log.i("onItemSelected: %s", parent);
        if(parent == binding.spinnerSido) {
            // Retrieve a new Sigun code list with the Sido given by by the Sido spinner.
            spinnerTask = ThreadManager2.getInstance().loadDistrictSpinnerTask(getContext(), opinetModel, position);
            // The Sigun spinner is set to the first position if the Sido spinner changes.
            if(mSidoItemPos != position) mSigunItemPos = 0;
            tmpSidoPos = position;

        } else tmpSigunPos = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // Should override onDialogClosed() defined in SpinnerDialogPreference to be invoked
    @Override
    public void onDialogClosed(boolean positive) {

        if(positive) {
            mSidoItemPos = tmpSidoPos;
            mSigunItemPos = tmpSigunPos;

            List<String> defaults = new ArrayList<>();
            defaults.add((String)sidoAdapter.getItem(mSidoItemPos));
            defaults.add(sigunAdapter.getItem(mSigunItemPos).getDistrictName());
            defaults.add(sigunAdapter.getItem(mSigunItemPos).getDistrictCode());

            // Share the district names with SettingPreferenceFragemnt to display the names in
            // the summary of the District preference.
            fragmentModel.getDefaultDistrict().setValue(defaults);
        }
    }

}
