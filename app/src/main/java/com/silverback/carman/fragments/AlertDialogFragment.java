package com.silverback.carman.fragments;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.core.View;
import com.silverback.carman.databinding.DialogAlertGeneralBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;

public class AlertDialogFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AlertDialogFragment.class);

    private static AlertDialogFragment alertFragment;
    private FragmentResultListener mListener;
    private FragmentSharedModel fragmentModel;
    private String title, message;
    private int category;

    private AlertDialogFragment() {
        // Required empty public constructor
    }

    // Singleton type constructor
    public static AlertDialogFragment newInstance(String title, String msg, int page) {
        if(alertFragment == null) alertFragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", msg);
        args.putInt("page", page);
        alertFragment.setArguments(args);

        return alertFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            title = getArguments().getString("title");
            message = getArguments().getString("message");
            category = getArguments().getInt("page");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = requireActivity().getLayoutInflater();
        DialogAlertGeneralBinding binding = DialogAlertGeneralBinding.inflate(inflater);
        binding.tvNumpadTitle.setText(title);
        binding.tvMessage.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        fragmentModel = new ViewModelProvider(this).get(FragmentSharedModel.class);
        builder.setView(binding.getRoot()).setPositiveButton("confirm", (dialog, which) -> {
            log.i("dialog: %s, %s", dialog, which);
            switch(category) {
                case Constants.GAS:
                    fragmentModel.setAlertGasResult(true);
                    break;

                case Constants.SVC:
                    fragmentModel.setAlertSvcResult(true);
                    break;

                case Constants.BOARD:
                    log.i("post removed notification: %s", dialog);
                    //fragmentModel.getAlertPostResult().setValue(true);
                    Bundle result = new Bundle();
                    result.putBoolean("alert", true);
                    getParentFragmentManager().setFragmentResult("confirmed", result);
                    break;
            }

            dismiss();

        }).setNegativeButton("cancel", (dialog, which) -> {
            //fragmentSharedModel.getAlertPostResult().setValue(false);
            dismiss();
        });

        return builder.create();
    }


}
