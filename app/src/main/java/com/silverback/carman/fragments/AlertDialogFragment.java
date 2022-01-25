package com.silverback.carman.fragments;


import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.databinding.DialogAlertGeneralBinding;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class AlertDialogFragment extends DialogFragment {

    // Logging
    //private static final LoggingHelper log = LoggingHelperFactory.create(AlertDialogFragment.class);

    private static AlertDialogFragment alertFragment;
    private FragmentSharedModel sharedModel;

    private String title, message;
    private int category;

    private AlertDialogFragment() {
        // Required empty public constructor
    }

    // Singleton type constructor
    public static AlertDialogFragment newInstance(String title, String msg, int category) {
        if(alertFragment == null) alertFragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", msg);
        args.putInt("category", category);
        alertFragment.setArguments(args);

        return alertFragment;
    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        if(getArguments() != null) {
            title = getArguments().getString("title");
            message = getArguments().getString("message");
            category = getArguments().getInt("category");
        }
    }
    //@SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = requireActivity().getLayoutInflater();
        DialogAlertGeneralBinding binding = DialogAlertGeneralBinding.inflate(inflater);
        binding.tvNumpadTitle.setText(title);
        binding.tvMessage.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot()).setPositiveButton("confirm", (dialog, which) -> {
            switch(category) {
                case Constants.GAS:
                    sharedModel.setAlertGasResult(true);
                    break;

                case Constants.SVC:
                    sharedModel.setAlertSvcResult(true);
                    break;

                case Constants.BOARD:
                    sharedModel.getAlertPostResult().setValue(true);
                    break;
            }
            dismiss();

        }).setNegativeButton("cancel", (dialog, which) -> {
            sharedModel.getAlertPostResult().setValue(false);
            dismiss();
        });

        return builder.create();
    }
}
