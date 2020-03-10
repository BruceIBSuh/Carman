package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingSvcItemDlgFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingSvcItemDlgFragment.class);

    // Objects
    private FragmentSharedModel sharedModel;
    private SharedPreferences mSettings;

    public SettingSvcItemDlgFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null) mSettings = ((SettingPreferenceActivity)getActivity()).getSettings();
    }


    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //LayoutInflater inflater = requireActivity().getLayoutInflater();
        //View localView = inflater.inflate(R.layout.dialog_setting_service, null);
        View localView = View.inflate(getContext(), R.layout.dialog_setting_service, null);

        EditText etItemName = localView.findViewById(R.id.et_item_name);
        EditText etMileage = localView.findViewById(R.id.et_period_km);
        EditText etMonth = localView.findViewById(R.id.et_period_month);
        boolean isMileage = false;

        etMileage.setOnFocusChangeListener((v, hasFocus) -> {
            if(!TextUtils.isEmpty(etMileage.getText()) && !hasFocus) {
                int avgMileage = Integer.valueOf(mSettings.getString(Constants.AVERAGE, "5000"));
                int calcMileage = (Integer.valueOf(etMileage.getText().toString()) / avgMileage) * 12;
                etMonth.setText(String.valueOf(calcMileage));
            }

        });


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setPositiveButton("confirm", (dialog, which) -> {

                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("name", etItemName.getText().toString());
                        jsonObject.put("mileage", etMileage.getText().toString());
                        jsonObject.put("month", etMonth.getText().toString());

                        ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class)
                                .setServiceItem(jsonObject);

                    } catch(JSONException e) {
                        log.e("JSONException: %s", e.getMessage());
                    }


                }).setNegativeButton("cancel", (dialog, which) -> {});

        return builder.create();

    }

}
