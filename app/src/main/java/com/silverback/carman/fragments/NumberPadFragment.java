package com.silverback.carman.fragments;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.silverback.carman.BaseActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.DialogNumberPadBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * This class subclasses DialogFragment to input numbers in a view of assocaited forms, no matter
 * what is currency or mileage, with params that receive a view title or unit name.
 */
public class NumberPadFragment extends DialogFragment implements View.OnClickListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(NumberPadFragment.class);

    // Constants
    private final String[] arrNumber = { "100", "50", "10", "1"};
    private final String[] arrCurrency = { "5만", "1만", "5천", "1천" };

    // Objects
    private DialogNumberPadBinding binding;
    private FragmentSharedModel fragmentModel;
    private DecimalFormat df;

    // UIs
    private TextView tvValue, tvUnit;
    private Button btnSign, btn1, btn2, btn3, btn4;

    // Fields
    private int viewId;
    private int selectedValue;
    private String initValue, itemLabel;
    private boolean isCurrency;
    private boolean isPlus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            viewId = getArguments().getInt("viewId");
            initValue = getArguments().getString("initValue");
            log.i("args: %s, %s", viewId, initValue);
        }

        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        df = BaseActivity.getDecimalFormatInstance();
        selectedValue = 0;
        isPlus = true;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //View localView = View.inflate(getContext(), R.layout.dialog_number_pad, null);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        binding = DialogNumberPadBinding.inflate(inflater);

        // Attach event listeners.
        //numberPadBinding.tvNumpadDefault.setOnClickListener(click -> numberPadBinding.tvNumpadDefault.setText(initValue));
        binding.btnNumpadSign.setOnClickListener(this);
        binding.btnNumpadBtn1.setOnClickListener(this);
        binding.btnNumpadBtn2.setOnClickListener(this);
        binding.btnNumpadBtn3.setOnClickListener(this);
        binding.btnNumpadBtn4.setOnClickListener(this);

        // Resource IDs will be non-final in Android Gradle Plugin version 7.0, avoid using them
        // in switch case statements
        if(viewId == R.id.tv_gas_mileage || viewId == R.id.tv_svc_mileage) {
            itemLabel = getString(R.string.exp_label_odometer);
            isCurrency = setInputNumberPad(arrNumber, getString(R.string.unit_km));
        } else if(viewId == R.id.tv_gas_payment) {
            itemLabel = getString(R.string.gas_label_expense);
            isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
        } else if(viewId == R.id.tv_gas_amount) {
            itemLabel = getString(R.string.gas_label_amount);
            isCurrency = setInputNumberPad(arrNumber, getString(R.string.unit_liter));
        } else if(viewId == R.id.tv_carwash) {
            itemLabel = getString(R.string.gas_label_expense_wash);
            isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
        } else if(viewId == R.id.tv_extra_payment) {
            itemLabel = getString(R.string.gas_label_expense_misc);
            isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
        } else if(viewId == R.id.tv_value_cost) {
            isCurrency = setInputNumberPad(arrCurrency, getString(R.string.unit_won));
        }

        binding.tvNumpadTitle.setText(itemLabel);
        binding.tvNumpadDefault.setText(initValue);

        // Set texts and values of the buttons on the pad in InputBtnPadView.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(binding.getRoot())
                .setPositiveButton("confirm", (dialog, which) ->
                    fragmentModel.setNumPadValue(viewId, selectedValue))
                .setNegativeButton("cancel", (dialog, which) -> {});

        return builder.create();

    }

    @Override
    public void onClick(View v) {
        int number = 0;
        try {
            selectedValue = Objects.requireNonNull(
                    df.parse(binding.tvNumpadDefault.getText().toString())).intValue();
        } catch (ParseException e) {
            log.e("ParseException: %s", e.getMessage());
        }

        if(v.getId() == R.id.btn_numpad_sign) {
            isPlus = !isPlus;
            String sign = (isPlus)? getString(R.string.sign_plus) : getString(R.string.sign_minus);
            binding.btnNumpadSign.setText(sign);
        } else if(v.getId() == R.id.btn_numpad_btn1) {
            number = (isCurrency) ? 50000 : 100;
        } else if(v.getId() == R.id.btn_numpad_btn2) {
            number = (isCurrency) ? 10000 : 50;
        } else if(v.getId() == R.id.btn_numpad_btn3) {
            number = (isCurrency) ? 5000 : 10;
        } else if(v.getId() == R.id.btn_numpad_btn4) {
            number = (isCurrency) ? 1000 : 1;
        }

        // Check if the sign is set to plus or minus and adds or substract a number unless the number
        // is under zero, in which the number reverts to zero and the sign is set to plus.
        if((selectedValue = (isPlus)? selectedValue + number : selectedValue - number) < 0) {
            selectedValue = 0;
            isPlus = !isPlus;
            binding.btnNumpadSign.setText((isPlus)?getString(R.string.sign_plus) : getString(R.string.sign_minus));
        }

        binding.tvNumpadDefault.setText(df.format(selectedValue));

    }

    // Set the button name and unit accroding to whether the unit is currency or amount
    private boolean setInputNumberPad(String[] name, String unit) {
        binding.btnNumpadBtn1.setText(name[0]);
        binding.btnNumpadBtn2.setText(name[1]);
        binding.btnNumpadBtn3.setText(name[2]);
        binding.btnNumpadBtn4.setText(name[3]);

        binding.tvNumpadUnit.setText(unit);

        return unit.equals(getString(R.string.unit_won));

    }

}
