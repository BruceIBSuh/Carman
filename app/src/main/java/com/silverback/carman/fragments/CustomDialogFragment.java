package com.silverback.carman.fragments;


import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.databinding.DialogAlertGeneralBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;

public class CustomDialogFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CustomDialogFragment.class);

    private static CustomDialogFragment alertFragment;
    private FragmentSharedModel fragmentModel;
    private String title, message;
    private int category;


    private CustomDialogFragment() {
        // Required empty public constructor
    }

    // Singleton type constructor
    public static CustomDialogFragment newInstance(String title, String msg, int page) {
        if(alertFragment == null) alertFragment = new CustomDialogFragment();
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
            fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
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
                    result.putBoolean("confirmed", true);
                    getParentFragmentManager().setFragmentResult("removePost", result);
                    break;
            }

            dismiss();

        }).setNegativeButton("cancel", (dialog, which) -> {
            //fragmentSharedModel.getAlertPostResult().setValue(false);
            log.i("cancel clicked");
            dismiss();
        });

        return builder.create();
    }


}
