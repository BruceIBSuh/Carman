package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    static ServiceItemMemoFragment newInstance(String name, int pos) {
        ServiceItemMemoFragment memoDlgFragment = new ServiceItemMemoFragment();
        Bundle args = new Bundle();
        args.putString("title", name);
        args.putInt("position", pos);
        memoDlgFragment.setArguments(args);

        return memoDlgFragment;
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
        //View localView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_service_item_memo, null);
        View localView = View.inflate(getContext(), R.layout.fragment_service_item_memo, null);
        // Prevent the Dialog from closing by clicking outside of the dialog
        setCancelable(false);

        // Set the dialog style: no title
        //if(getDialog().getWindow() != null) getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final String itemName = getArguments().getString("title");
        final int itemPosition = getArguments().getInt("position");

        TextView tvItemName = localView.findViewById(R.id.tv_item_title);
        EditText etMemo = localView.findViewById(R.id.et_item_memo);
        etMemo.requestFocus();
        tvItemName.setText(itemName);

        localView.findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                String memo = etMemo.getText().toString();
                //mListener.inputMemotext(memo, itemPosition);
                dismiss();
            }
        });

        localView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        // Define UI's


        return new AlertDialog.Builder(getActivity())
                .setView(localView)
                .create();
    }

}
