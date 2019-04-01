package com.silverback.carman2.views;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;

import java.util.List;

import androidx.preference.DialogPreference;

public class SpinnerDialogPreference extends DialogPreference {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerDialogPreference.class);

    // Constants
    private final int mDialogLayoutResId = R.layout.dialogpref_spinner;


    // Objects
    private ArrayAdapter sidoAdapter;
    private DistrictSpinnerAdapter sigunAdapter;

    public SpinnerDialogPreference(Context context) {
        super(context);
    }

    public SpinnerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    public SpinnerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getAttributes(context, attrs);
    }

    public SpinnerDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getAttributes(context, attrs);
    }

    private void getAttributes(Context context, AttributeSet attrs) {

        setDialogLayoutResource(mDialogLayoutResId);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String distCode = null;


        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpinnerDialogPreference);
        try {
            String defaultCode = typedArray.getString(R.styleable.SpinnerDialogPreference_siguncode);
            log.i("Default Value: %s", defaultCode);
            distCode = sharedPreferences.getString(Constants.DISTRICT, defaultCode);

        } finally {
            typedArray.recycle();
        }


        // Create sidoAdapter using ArrayAdapter, and sigunAdapter using a BaseAdapter extending
        // cusom adapter.
        sidoAdapter = ArrayAdapter.createFromResource(
                context, R.array.sido_name, android.R.layout.simple_spinner_item);
        sidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sigunAdapter = new DistrictSpinnerAdapter(getContext());


    }


    public void onDialogClosed(boolean positiveResult) { }


    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }


    public ArrayAdapter getSidoAdapter() {
        return sidoAdapter;
    }

    public DistrictSpinnerAdapter getSigunAdapter() {
        return sigunAdapter;
    }

}
