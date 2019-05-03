package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.api.Logging;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class InputPadFragment extends DialogFragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(InputPadFragment.class);

    // Objects
    private FragmentSharedModel viewModel;


    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_input_pad, container, false);
    }
    */


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);

        String title = null;
        String value, unit;
        boolean category;

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View localView = inflater.inflate(R.layout.fragment_input_pad, null);

        final TextView tvValue = localView.findViewById(R.id.defaultValue);
        TextView tvUnit = localView.findViewById(R.id.unit);
        Button btn1 = localView.findViewById(R.id.padButton1);
        Button btn2 = localView.findViewById(R.id.padButton2);
        Button btn3 = localView.findViewById(R.id.padButton3);
        Button btn4 = localView.findViewById(R.id.padButton4);


        if(getArguments() != null) {
            value = getArguments().getString("value");
            unit = getArguments().getString("unit");
            title = getArguments().getString("title");
            category = getArguments().getBoolean("category");

            tvValue.setText(value);
            tvUnit.setText(unit);
            if(category) {
                btn1.setText("100");
                btn2.setText("50");
                btn3.setText("10");
                btn4.setText("1");
            } else {
                btn1.setText("5만");
                btn2.setText("1만");
                btn3.setText("5천");
                btn4.setText("1천");
            }
        }



        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setView(localView)
                .setPositiveButton("confirm", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        log.i("which: %s", which);
                        viewModel.setInputValue(tvValue.getText().toString());
                    }
                })

                .setNegativeButton("cancel", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        return builder.create();
    }

}
