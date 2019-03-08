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

import com.silverback.carman2.R;
import com.silverback.carman2.opinet.AvgPriceView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment {

    // Constants

    // Objects

    // UI's
    private Spinner fuelSpinner;
    private FrameLayout frameAvgPrice, frameSidoPrice, frameSigunPrice;


    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View childView = inflater.inflate(R.layout.fragment_main, container, false);

        // UI's
        frameAvgPrice = childView.findViewById(R.id.frame_price_avg);
        frameSidoPrice = childView.findViewById(R.id.frame_price_sido);
        frameSigunPrice = childView.findViewById(R.id.frame_price_sigun);

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
        /*
        for(int i = 0; i < code.length; i++) {
            if(code[i].matches(defaultParams[0])){
                spinner.setSelection(i);
                break;
            }
        }
        */
        // Attach the listener to the spinner
        //fuelSpinner.setOnItemSelectedListener(this);

        // Adds the Custom View of AvgPriceView to the FrameLayout of frameAvgPrice
        //AvgPriceView avg = new AvgPriceView(getContext());
        //frameAvgPrice.addView(avg);

        // Inflate the layout for this fragment
        return childView;
    }

    // Formats date and time with milliseconds
    public static String formatMilliseconds(String format, long milliseconds) {
        //Date date = new Date(milliseconds);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(milliseconds);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(calendar.getTime());

    }

}
