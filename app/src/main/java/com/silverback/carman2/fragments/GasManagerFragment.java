package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.CustomPagerIndicator;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

/**
 * A simple {@link Fragment} subclass.
 */
public class GasManagerFragment extends Fragment implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Objects
    private DecimalFormat df;
    private FragmentSharedModel viewModel;
    private TabLayout tabLayout;
    private ExpensePagerAdapter viewPagerAdapter;
    private CustomPagerIndicator indicator;
    private Calendar calendar;
    private SimpleDateFormat sdf;
    private InputPadFragment padDialog;

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

        if(getActivity() != null) {
            viewModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        }

        df = BaseActivity.getDecimalFormatInstance();

        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_gas, container, false);

        // Set the current date and time
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

        /**
         * Introduce ViewModel to communicate between parent Fragment and AlertFragment
         * Set Observier to ViewModel(Lamda expression available, instead).
         */
        viewModel.getInputValue().observe(this, new Observer<String>(){
            @Override
            public void onChanged(String data) {
                log.i("viewMode value:%s", data);
                try {
                    int value = (df.parse(data)).intValue();
                    tvOdometer.setText(df.format(value));
                } catch(ParseException e) {
                    log.e("ParseException: %s", e.getMessage());
                }

            }
        });


        return localView;
    }


    @Override
    public void onClick(View v) {
        Bundle args = new Bundle();
        padDialog = new InputPadFragment();

        // Pass the current saved value to InputPadFragment
        switch(v.getId()) {
            case R.id.tv_mileage:
                args.putString("value", tvOdometer.getText().toString());
                break;

            case R.id.tv_payment:
                args.putString("value", tvGasPaid.getText().toString());
                break;

            case R.id.tv_amount:
                args.putString("value", tvGasLoaded.getText().toString());
                break;

            case R.id.tv_carwash:
                args.putString("value", tvCarwashPaid.getText().toString());
                break;

            case R.id.tv_extra:
                args.putString("value", tvExtraPaid.getText().toString());
                break;
        }

        // Pass the id of TextView to InputPadFragment for which TextView is being focused to wait
        // for a new value.
        args.putInt("viewId", v.getId());
        padDialog.setArguments(args);
        
        if(getFragmentManager() != null) padDialog.show(getFragmentManager(), "InputPadDialog");
    }

}
