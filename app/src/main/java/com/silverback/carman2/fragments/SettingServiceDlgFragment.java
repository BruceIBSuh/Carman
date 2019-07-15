package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingServiceDlgFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingServiceDlgFragment.class);

    // Objects
    private FragmentSharedModel sharedModel;

    public SettingServiceDlgFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null)
            sharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
    }


    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View localView = inflater.inflate(R.layout.dialog_setting_service, null);

        EditText etItemName = localView.findViewById(R.id.et_item_name);
        EditText etPeriodKm = localView.findViewById(R.id.et_period_km);
        EditText etPeriodMonth = localView.findViewById(R.id.et_period_month);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setPositiveButton("confirm", (dialog, which) -> {
                        String[] arrItem = {
                                etItemName.getText().toString(),
                                etPeriodKm.getText().toString(),
                                etPeriodMonth.getText().toString()};
                        final List<String> itemInfo = Arrays.asList(arrItem);
                        sharedModel.setServiceItem(itemInfo);
                })
                .setNegativeButton("cancel", (dialog, which) -> {});

        return builder.create();

    }

}
