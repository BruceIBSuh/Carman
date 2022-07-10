package com.silverback.carman.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;

import com.silverback.carman.R;
import com.silverback.carman.adapters.SigunSpinnerAdapter;
import com.silverback.carman.databinding.DialogSettingSpinnerBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.DistDownloadRunnable;
import com.silverback.carman.threads.DistSpinnerTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.OpinetViewModel;

import java.util.ArrayList;
import java.util.List;

public class SettingSpinnerFragment extends DialogFragment implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingSpinnerFragment.class);

    // Objects
    private Preference pref;
    private DialogSettingSpinnerBinding binding;
    private OpinetViewModel opinetModel;
    private DistSpinnerTask spinnerTask;
    private ArrayAdapter<CharSequence> sidoAdapter;
    private SigunSpinnerAdapter sigunAdapter;
    private FragmentSharedModel fragmentModel;
    // Fields
    private int mSidoItemPos, mSigunItemPos, tmpSidoPos, tmpSigunPos;
    private String distCode;

    public SettingSpinnerFragment(Preference pref, String distCode) {
        this.pref = pref;
        this.distCode = distCode;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        binding = DialogSettingSpinnerBinding.inflate(getLayoutInflater());
        binding.spinnerSido.setOnItemSelectedListener(this);
        binding.spinnerSigun.setOnItemSelectedListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        AlertDialog dialog = builder.setView(binding.getRoot())
                .setTitle(R.string.pref_dialog_district_title)
                .setPositiveButton(getString(R.string.dialog_btn_confirm), (v,which) -> setSigunSpinnerList())
                .setNegativeButton(getString(R.string.dialog_btn_cancel), (v, which) -> dismiss())
                .create();
        dialog.show();

        // Integer.valueOf("01") fortunately translates into 1^^.
        if(!TextUtils.isEmpty(distCode)){
            int sidoCode = Integer.parseInt(distCode.substring(0, 2));
            mSidoItemPos = (sidoCode < 14) ? sidoCode - 1 : sidoCode - 3;
        }

        sidoAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sido_name, R.layout.spinner_settings_entry);
        sidoAdapter.setDropDownViewResource(R.layout.spinner_settings_dropdown);
        binding.spinnerSido.setAdapter(sidoAdapter);
        binding.spinnerSido.setSelection(mSidoItemPos, true);
        sigunAdapter = new SigunSpinnerAdapter(getContext());

        opinetModel.getSpinnerDataList().observe(this, sigunList -> {
            log.i("opinetModel: %s", sigunList);
            if(sigunAdapter.getCount() > 0) sigunAdapter.removeAll();
            sigunAdapter.addSigunList(sigunList);
            if(mSidoItemPos != tmpSidoPos) mSigunItemPos = 0;
            else {
                int position = 0;
                for(DistDownloadRunnable.Area code :  sigunList) {
                    if (code.getAreaCd().equals(distCode)) mSigunItemPos = position;
                    position++;
                }
            }
            binding.spinnerSigun.setAdapter(sigunAdapter);
            binding.spinnerSigun.setSelection(mSigunItemPos, true);
        });

        return dialog;
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        if(adapterView == binding.spinnerSido) {
            // Retrieve a new Sigun code list with the Sido given by by the Sido spinner.
            spinnerTask = ThreadManager2.loadDistSpinnerTask(getContext(), opinetModel, pos);
            // The Sigun spinner is set to the first position if the Sido spinner changes.
            if(mSidoItemPos != pos) mSigunItemPos = 0;
            tmpSidoPos = pos;
        } else tmpSigunPos = pos;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    private void setSigunSpinnerList() {
        mSidoItemPos = tmpSidoPos;
        mSigunItemPos = tmpSigunPos;

        List<String> defaults = new ArrayList<>();
        defaults.add((String)sidoAdapter.getItem(mSidoItemPos));
        defaults.add(sigunAdapter.getItem(mSigunItemPos).getAreaName());
        defaults.add(sigunAdapter.getItem(mSigunItemPos).getAreaCd());

        // Share the district names with SettingPreferenceFragemnt to display the names in
        // the summary of the District preference.
        fragmentModel.getDefaultDistrict().setValue(defaults);
    }
}
