package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * A simple {@link Fragment} subclass.
 * This class subclasses DialogFragment to input numbers in a view of assocaited forms, no matter
 * what is currency or mileage, with params that receive a view title or unit name.
 */
public class NumberPadFragment extends DialogFragment implements View.OnClickListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(NumberPadFragment.class);

    // Constants
    private String[] arrNumber = { "100", "50", "10", "1"};
    private String[] arrCurrency = { "5만", "1만", "5천", "1천" };

    // Objects
    private FragmentSharedModel fragmentModel;
    private DecimalFormat df;

    // UIs
    private TextView tvValue, tvUnit;
    private Button btnSign, btn1, btn2, btn3, btn4;

    // Fields
    private int textViewId;
    private int selectedValue;
    private String initValue, itemLabel;
    private boolean isCurrency;
    private boolean isPlus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null)
            fragmentModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);

        if(getArguments() != null) {
            itemLabel = getArguments().getString("itemLabel");
            textViewId = getArguments().getInt("viewId");
            initValue = getArguments().getString("initValue");
            log.i("Dialog: %s, %s, %s", itemLabel, textViewId, initValue);
        }

        df = BaseActivity.getDecimalFormatInstance();
        selectedValue = 0;
        isPlus = true;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View localView = View.inflate(getContext(), R.layout.dialog_number_pad, null);

        TextView tvTitle = localView.findViewById(R.id.tv_title);
        tvValue = localView.findViewById(R.id.defaultValue);
        tvUnit = localView.findViewById(R.id.unit);
        btnSign = localView.findViewById(R.id.btn_sign);
        btn1 = localView.findViewById(R.id.padButton1);
        btn2 = localView.findViewById(R.id.padButton2);
        btn3 = localView.findViewById(R.id.padButton3);
        btn4 = localView.findViewById(R.id.padButton4);

        tvValue.setText(initValue);
        tvValue.setOnClickListener(event -> tvValue.setText(initValue));

        btnSign.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);

        // Get arguments from the parent activity as to dialog title, unit name, and button numbers.
        switch(textViewId) {
            // This case is shared by Gas and Service in common.
            case R.id.tv_mileage:
                itemLabel = getString(R.string.exp_label_odometer);
                isCurrency = setInputNumberPad(arrNumber, getString(R.string.unit_km));
                break;

            case R.id.tv_gas_payment:
                itemLabel = getString(R.string.gas_label_expense);
                isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
                break;

            case R.id.tv_gas_amount:
                itemLabel = getString(R.string.gas_label_amount);
                isCurrency = setInputNumberPad(arrNumber, getString(R.string.unit_liter));
                break;

            case R.id.tv_carwash_payment:
                itemLabel = getString(R.string.gas_label_expense_wash);
                isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
                break;

            case R.id.tv_extra_payment:
                itemLabel = getString(R.string.gas_label_expense_misc);
                isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
                break;

            // This case is only applied to Service Items, the name of which is passed by
            // the argument under the name of "itemLabel"
            case R.id.tv_value_cost:
                isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
                break;

            default: break;
        }


        tvTitle.setText(itemLabel);
        tvValue.setText(initValue);

        // Set texts and values of the buttons on the pad in InputBtnPadView.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(localView)
                .setPositiveButton("confirm", (dialog, which) ->
                    fragmentModel.setSelectedValue(textViewId, selectedValue))
                .setNegativeButton("cancel", (dialog, which) -> {});


        return builder.create();

    }

    @Override
    public void onClick(View v) {

        int number = 0;

        try {
            selectedValue = df.parse(tvValue.getText().toString()).intValue();
        } catch (ParseException e) {
            log.e("ParseException: %s", e.getMessage());
        }

        switch(v.getId()) {
            case R.id.btn_sign:
                isPlus = !isPlus;
                String sign = (isPlus)? getString(R.string.sign_plus) : getString(R.string.sign_minus);
                btnSign.setText(sign);
                break;

            case R.id.padButton1:
                number = (isCurrency) ? 50000 : 100;
                break;

            case R.id.padButton2:
                number = (isCurrency) ? 10000 : 50;
                break;

            case R.id.padButton3:
                number = (isCurrency) ? 5000 : 10;
                break;

            case R.id.padButton4:
                number = (isCurrency) ? 1000 : 1;
                break;
        }

        // Check if the sign is set to plus or minus and adds or substract a number unless the number
        // is under zero, in which the number reverts to zero and the sign is set to plus.
        if((selectedValue = (isPlus)? selectedValue + number : selectedValue - number) < 0) {
            selectedValue = 0;
            isPlus = !isPlus;
            btnSign.setText((isPlus)?getString(R.string.sign_plus) : getString(R.string.sign_minus));
        }

        tvValue.setText(df.format(selectedValue));

    }

    // Set the button name and unit accroding to whether the unit is currency or amount
    private boolean setInputNumberPad(String[] name, String unit) {
        btn1.setText(name[0]);
        btn2.setText(name[1]);
        btn3.setText(name[2]);
        btn4.setText(name[3]);

        tvUnit.setText(unit);

        return unit.equals(getString(R.string.unit_won));

    }

}
