package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceItemMemoFragment extends DialogFragment {


    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemMemoFragment.class);

    // Objects
    private FragmentSharedModel sharedModel;

    public ServiceItemMemoFragment() {
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
    public Dialog onCreateDialog(Bundle bundle) {
        //View localView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_service_memo, null);
        View localView = View.inflate(getContext(), R.layout.dialog_service_memo, null);

        // Prevent the Dialog from closing by clicking outside of the dialog
        setCancelable(false);

        // Set the dialog style: no title
        //if(getDialog().getWindow() != null) getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final String itemName = getArguments().getString("title");
        final int viewId = getArguments().getInt("viewId");

        TextView tvItemName = localView.findViewById(R.id.tv_memo_title);
        EditText etMemo = localView.findViewById(R.id.et_item_memo);
        etMemo.requestFocus();
        tvItemName.setText(itemName);

        /*
        localView.findViewById(R.id.btn_confirm).setOnClickListener(view -> {
            String memo = etMemo.getText().toString();
            sharedModel.setSelectedMemo(viewId, memo);
            dismiss();
        });

        localView.findViewById(R.id.btn_cancel).setOnClickListener(view -> dismiss());
        */

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setPositiveButton(R.string.dialog_btn_confirm, (dialog, which) -> {
                    String memo = etMemo.getText().toString();
                    sharedModel.setSelectedMemo(viewId, memo);
                    dismiss();
                })
                .setNegativeButton(R.string.dialog_btn_cancel, (dialog, which) -> dismiss());

        return builder.create();

    }

}
