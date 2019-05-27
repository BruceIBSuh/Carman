package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.view.LayoutInflater;
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
 */
public class InputPadFragment extends DialogFragment implements View.OnClickListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(InputPadFragment.class);

    // Constants
    private String[] arrNumber = { "100", "50", "10", "1"};
    private String[] arrCurrency = { "5만", "1만", "5천", "1천" };

    // Objects
    private FragmentSharedModel viewModel;
    private static DecimalFormat df;

    // UIs
    private TextView tvValue, tvUnit;
    private Button btnSign, btn1, btn2, btn3, btn4;

    // Fields
    private int textViewId;
    private String initValue;
    private boolean isCurrency;
    private boolean isPlus = true;

    static {
        df = BaseActivity.getDecimalFormatInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null)
            viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);

        if(getArguments() != null) {
            textViewId = getArguments().getInt("viewId");
            initValue = getArguments().getString("value");
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View localView = inflater.inflate(R.layout.fragment_input_pad, null);

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

        // Get arguments from the parent activity dialog title, unit name, and button numbers.
        String title = null;
        switch(textViewId) {

            case R.id.tv_mileage:
                title = getString(R.string.exp_label_odometer);
                //tvValue.setText(getArguments().getString("value"));
                isCurrency = setInputNumberPad(arrNumber, getString(R.string.unit_km));
                break;

            case R.id.tv_payment:
                title = getString(R.string.gas_label_expense);
                //tvValue.setText(getArguments().getString("value"));
                isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
                break;

            case R.id.tv_amount:
                title = getString(R.string.gas_label_amount);
                ///tvValue.setText(getArguments().getString("value"));
                isCurrency = setInputNumberPad(arrNumber, getString(R.string.unit_liter));
                break;

            case R.id.tv_carwash:
                title = getString(R.string.gas_label_expense_wash);
                //tvValue.setText(getArguments().getString("value"));
                isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
                break;

            case R.id.tv_extra:
                title = getString(R.string.gas_label_expense_misc);
                //tvValue.setText(getArguments().getString("value"));
                isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
                break;
        }

        tvTitle.setText(title);

        // Set texts and values of the buttons on the pad in InputBtnPadView.
        //btnPad.initPad(getArguments().getInt("viewId"));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder//.setTitle(title)
                .setView(localView)
                .setPositiveButton("confirm", (dialog, which) ->
                        viewModel.setInputValue(tvValue.getText().toString()))

                .setNegativeButton("cancel", (dialog, which) -> {});


        return builder.create();
    }

    @Override
    public void onClick(View v) {

        int value = 0;
        int number = 0;

        try {
            value = df.parse(tvValue.getText().toString()).intValue();
        } catch (ParseException e) {
            log.e("ParseException: %s", e.getMessage());
        }

        switch(v.getId()) {

            case R.id.btn_sign:
                isPlus = !isPlus;
                String sign = (isPlus)? "+" : "-";
                btnSign.setText(sign);
                break;

            case R.id.padButton1:
                number = (isCurrency)?50000:100;
                break;

            case R.id.padButton2:
                number = (isCurrency)?10000:50;
                break;

            case R.id.padButton3:
                number = (isCurrency)?5000:10;
                break;

            case R.id.padButton4:
                number = (isCurrency)?1000:1;
                break;
        }

        value = (isPlus)? value + number : value - number;
        tvValue.setText(df.format(value));

    }

    private boolean setInputNumberPad(String[] name, String unit) {
        btn1.setText(name[0]);
        btn2.setText(name[1]);
        btn3.setText(name[2]);
        btn4.setText(name[3]);

        tvUnit.setText(unit);

        return unit.equals(getString(R.string.unit_won));

    }

}
