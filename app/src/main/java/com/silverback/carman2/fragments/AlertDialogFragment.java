package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class AlertDialogFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AlertDialogFragment.class);

    // Objects
    private static AlertDialogFragment alertDialogFragment;
    private FragmentSharedModel fragmentSharedModel;
    private String title, message;

    private AlertDialogFragment() {
        // Required empty public constructor
    }

    // Singleton type constructor
    static AlertDialogFragment newInstance(String title, String msg) {
        if(alertDialogFragment == null) alertDialogFragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", msg);
        alertDialogFragment.setArguments(args);

        return alertDialogFragment;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragmentSharedModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        title = getArguments().getString("title");
        message = getArguments().getString("message");

    }


    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        //View localView = inflater.inflate(R.layout.dialog_alert_general, null);
        View localView = View.inflate(getContext(), R.layout.dialog_alert_general, null);

        TextView tvTitle = localView.findViewById(R.id.tv_title);
        TextView tvMessage = localView.findViewById(R.id.tv_message);

        tvTitle.setText(title);
        tvMessage.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setPositiveButton("confirm", (dialog, which) -> fragmentSharedModel.setAlert(true))
                .setNegativeButton("cancel", (dialog, which) -> fragmentSharedModel.setAlert(false));

        return builder.create();
    }


}
