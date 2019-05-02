package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.CustomPagerIndicator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class GasManagerFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Constants
    private static final int NumOfPages = 5;

    // Objects
    private TabLayout tabLayout;
    private ExpensePagerAdapter viewPagerAdapter;
    private CustomPagerIndicator indicator;
    private Calendar calendar;
    private SimpleDateFormat sdf;

    // UIs
    private TextView tvOdometer, tvDateTime, tvGasPaid, tvGasLoaded, tvCarwashPaid, tvExtraPaid;
    private EditText etStnName, etUnitPrice, etExtraPxpense;

    // Fields

    public GasManagerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_gas, container, false);

        TextView tvDate = localView.findViewById(R.id.tv_date_time);

        calendar = Calendar.getInstance(Locale.getDefault());
        sdf = new SimpleDateFormat(getString(R.string.date_format_1), Locale.getDefault());

        // Set the current date and time
        String date = BaseActivity.formatMilliseconds(
                getString(R.string.date_format_1), System.currentTimeMillis());
        tvDate.setText(date);


        return localView;
    }
}
