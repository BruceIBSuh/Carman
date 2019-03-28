package com.silverback.carman2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SigunSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.core.view.LayoutInflaterCompat;
import androidx.preference.DialogPreference;

public class DistrictDialogPreference extends DialogPreference implements
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DistrictDialogPreference.class);

    // Objects
    private Spinner sidoSpinner, sigunSpinner;
    private ArrayAdapter sidoAdapter;
    private SigunSpinnerAdapter sigunAdapter;



    // Constructor
    public DistrictDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.spinners_district);

        sidoAdapter = ArrayAdapter.createFromResource(
                context, R.array.sido_name, android.R.layout.simple_spinner_item);
        sidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sigunAdapter = new SigunSpinnerAdapter(context);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
