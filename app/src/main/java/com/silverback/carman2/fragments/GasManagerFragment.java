package com.silverback.carman2.fragments;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.CustomPagerIndicator;
import com.silverback.carman2.views.InputBtnPadView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

/**
 * A simple {@link Fragment} subclass.
 */
public class GasManagerFragment extends Fragment implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Constants
    private static final int NumOfPages = 5;

    // Objects
    private FragmentSharedModel viewModel;
    private TabLayout tabLayout;
    private ExpensePagerAdapter viewPagerAdapter;
    private CustomPagerIndicator indicator;
    private Calendar calendar;
    private SimpleDateFormat sdf;
    private InputBtnPadView pad1, pad2, pad3, pad4, pad5;
    private InputPadFragment padDialog;

    // UIs
    private TextView tvOdometer, tvDateTime, tvGasPaid, tvGasLoaded, tvCarwashPaid, tvExtraPaid;
    private EditText etStnName, etUnitPrice, etExtraPxpense;
    // Fields

    public GasManagerFragment() {
        // Required empty public constructor

        //viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_gas, container, false);

        TextView tvDate = localView.findViewById(R.id.tv_date_time);
        calendar = Calendar.getInstance(Locale.getDefault());
        sdf = new SimpleDateFormat(getString(R.string.date_format_1), Locale.getDefault());
        String date = BaseActivity.formatMilliseconds(
                getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);


        // Attach Click Listener to TextViews
        tvOdometer = localView.findViewById(R.id.tv_mileage);
        tvGasPaid = localView.findViewById(R.id.tv_payment);
        tvGasLoaded = localView.findViewById(R.id.tv_amount);
        tvCarwashPaid = localView.findViewById(R.id.tv_carwash);
        tvExtraPaid = localView.findViewById(R.id.tv_extra);

        tvOdometer.setOnClickListener(this);
        tvGasPaid.setOnClickListener(this);
        tvGasLoaded.setOnClickListener(this);
        tvCarwashPaid.setOnClickListener(this);
        tvExtraPaid.setOnClickListener(this);

        // Set Observer to ViewMode(Lamda expression available, instead)
        /*
        viewModel.getInputValue().observe(this, new Observer<String>(){
            @Override
            public void onChanged(String s) {
                tvOdometer.setText(s);
            }
        });
        */

        return localView;
    }


    @Override
    public void onClick(View v) {
        Bundle args = new Bundle();
        padDialog = new InputPadFragment();

        switch(v.getId()) {
            case R.id.tv_mileage:
                args.putString("title", getString(R.string.gas_label_odometer));
                args.putString("unit", getString(R.string.unit_km));
                args.putString("value", tvOdometer.getText().toString());
                args.putBoolean("category", true); // true: number false-currency

                break;

            case R.id.tv_payment:
                args.putString("title", getString(R.string.gas_label_expense_gas));
                args.putString("unit", getString(R.string.unit_won));
                args.putString("value", tvGasPaid.getText().toString());
                args.putBoolean("category", false);
                break;

            case R.id.tv_amount:
                args.putString("title", getString(R.string.gas_label_amount));
                args.putString("unit", getString(R.string.unit_liter));
                args.putString("value", tvGasLoaded.getText().toString());
                args.putBoolean("category", true);
                break;

            case R.id.tv_carwash:
                args.putString("title", getString(R.string.gas_label_expense_wash));
                args.putString("unit", getString(R.string.unit_won));
                args.putString("value", tvCarwashPaid.getText().toString());
                args.putBoolean("category", false);
                break;

            case R.id.tv_extra:
                args.putString("title", getString(R.string.gas_label_expense_misc));
                args.putString("unit", getString(R.string.unit_won));
                args.putString("value", tvExtraPaid.getText().toString());
                args.putBoolean("category", false);
                break;

        }

        padDialog.setArguments(args);
        if(getFragmentManager() != null) padDialog.show(getFragmentManager(), "InputPadDialog");
    }

}
