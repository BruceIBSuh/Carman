package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.View;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 */
public class AlertDialogFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AlertDialogFragment.class);

    // Objects
    private static AlertDialogFragment alertFragment;
    private FragmentSharedModel sharedModel;
    private String title, message;
    private int category;

    private AlertDialogFragment() {
        // Required empty public constructor
    }

    // Singleton type constructor
    static AlertDialogFragment newInstance(String title, String msg, int category) {
        if(alertFragment == null) alertFragment = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", msg);
        args.putInt("category", category);
        alertFragment.setArguments(args);

        return alertFragment;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        title = getArguments().getString("title");
        message = getArguments().getString("message");
        category = getArguments().getInt("category");
    }


    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //LayoutInflater inflater = requireActivity().getLayoutInflater();
        //View localView = inflater.inflate(R.layout.dialog_alert_general, null);
        View localView = View.inflate(getContext(), R.layout.dialog_alert_general, null);

        TextView tvTitle = localView.findViewById(R.id.tv_title);
        TextView tvMessage = localView.findViewById(R.id.tv_message);

        tvTitle.setText(title);
        tvMessage.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView).setPositiveButton("confirm", (dialog, which) -> {
            switch(category) {
                case Constants.GAS:
                    sharedModel.setAlertGasResult(true);
                    break;

                case Constants.SVC:
                    sharedModel.setAlertSvcResult(true);
                    break;

                case Constants.POST:
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
