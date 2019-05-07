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
import android.widget.Button;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class InputPadFragment extends DialogFragment implements View.OnClickListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(InputPadFragment.class);

    // Constants
    private String[] arrNumber = { "100", "50", "10", "1"};
    private String[] arrCurrency = { "5만", "1만", "5천", "1천" };

    // Objects
    private FragmentSharedModel viewModel;

    // UIs
    private TextView tvValue;
    private Button btn1, btn2, btn3, btn4;

    // Fields
    private int inputValue;

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if(getActivity() != null) {
            viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        }

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View localView = inflater.inflate(R.layout.fragment_input_pad, null);

        TextView tvUnit = localView.findViewById(R.id.unit);
        tvValue = localView.findViewById(R.id.defaultValue);
        btn1 = localView.findViewById(R.id.padButton1);
        btn2 = localView.findViewById(R.id.padButton2);
        btn3 = localView.findViewById(R.id.padButton3);
        btn4 = localView.findViewById(R.id.padButton4);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);

        String title = null;

        switch(getArguments().getInt("viewId")) {
            case R.id.tv_mileage:
                title = getString(R.string.gas_label_odometer);
                tvValue.setText(getArguments().getString("value"));
                tvUnit.setText(getString(R.string.unit_km));
                setButtonName(arrNumber);
                break;

            case R.id.tv_payment:
                title = getString(R.string.gas_label_expense_gas);
                tvValue.setText(getArguments().getString("value"));
                tvUnit.setText(getString(R.string.unit_won));
                setButtonName(arrCurrency);
                break;

            case R.id.tv_amount:
                title = getString(R.string.gas_label_amount);
                tvValue.setText(getArguments().getString("value"));
                tvUnit.setText(getString(R.string.unit_liter));
                setButtonName(arrNumber);
                break;

            case R.id.tv_carwash:
                title = getString(R.string.gas_label_expense_wash);
                tvValue.setText(getArguments().getString("value"));
                tvUnit.setText(getString(R.string.unit_won));
                setButtonName(arrCurrency);
                break;

            case R.id.tv_extra:
                title = getString(R.string.gas_label_expense_misc);
                tvValue.setText(getArguments().getString("value"));
                tvUnit.setText(getString(R.string.unit_won));
                setButtonName(arrCurrency);
                break;
        }

        // Set texts and values of the buttons on the pad in InputBtnPadView.
        //btnPad.initPad(getArguments().getInt("viewId"));

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

    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.padButton1:
                inputValue += 100;
                break;

            case R.id.padButton2:
                inputValue += 50;
                break;

            case R.id.padButton3:
                inputValue += 10;
                break;

            case R.id.padButton4:
                inputValue += 1;
                break;

        }

        tvValue.setText(String.valueOf(inputValue));

    }

    private void setButtonName(String[] name) {
        btn1.setText(name[0]);
        btn2.setText(name[1]);
        btn3.setText(name[2]);
        btn4.setText(name[3]);
    }
}
