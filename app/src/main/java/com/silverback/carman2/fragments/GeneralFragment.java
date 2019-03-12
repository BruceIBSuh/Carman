package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.threads.OpinetPriceTask;
import com.silverback.carman2.threads.ThreadManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.silverback.carman2.BaseActivity.formatMilliseconds;

/**
 * A simple {@link Fragment} subclass.
 */
public class GeneralFragment extends Fragment {

    // Constants

    // Objects
    private OpinetPriceTask priceTask;

    // UI's
    private Spinner fuelSpinner;
    private FrameLayout frameAvgPrice;

    // Fields
    private String[] defaultParams;
    private String jsonString;

    public GeneralFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] district = getResources().getStringArray(R.array.default_district);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View childView = inflater.inflate(R.layout.fragment_general, container, false);

        // UI's
        frameAvgPrice = childView.findViewById(R.id.fl_opinet_avg);

        TextView tvDate = childView.findViewById(R.id.tv_date);
        fuelSpinner = childView.findViewById(R.id.spinner_fuel);

        String date = formatMilliseconds(getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);

        // Set the spinner_stat default value if it is saved in SharedPreference.Otherwise, sets it to 0.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_fuel_name, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        fuelSpinner.setAdapter(spinnerAdapter);

        // Set the spinner to the default value that's fetched from SharedPreferences
        String[] code = getResources().getStringArray(R.array.spinner_fuel_code);
        String[] defaultParams = getArguments().getStringArray("defaults");
        for(int i = 0; i < code.length; i++) {
            if(code[i].matches(defaultParams[0])){
                fuelSpinner.setSelection(i);
                break;
            }
        }

        // Attach the listener to the spinner
        //fuelSpinner.setOnItemSelectedListener(this);

        // Inflate the layout for this fragment
        return childView;
    }



}
