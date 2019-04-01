package com.silverback.carman2.views;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;

import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;

public class SpinnerDialogPreference extends DialogPreference implements
        Preference.OnPreferenceChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerDialogPreference.class);

    // Constants
    private final int mDialogLayoutResId = R.layout.dialogpref_spinner;

    // Objects
    private ArrayAdapter sidoAdapter;
    private DistrictSpinnerAdapter sigunAdapter;
    private List<Opinet.DistrictCode> distCodeList;

    // Constructors
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

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpinnerDialogPreference);
        try {
            String defaultCode = typedArray.getString(R.styleable.SpinnerDialogPreference_siguncode);
            log.i("Default Value: %s", defaultCode);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        log.i("onPreferenceChange: %s, %s", preference, newValue);
        return false;
    }

    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

    // Abstrct method which PreferenceDialogFragmentCompat overrides
    public void onDialogClosed(boolean positiveResult){
        log.i("onDialogClosed: %s", positiveResult);
    }


    public ArrayAdapter getSidoAdapter() {
        return sidoAdapter;
    }
    public DistrictSpinnerAdapter getSigunAdapter() {
        return sigunAdapter;
    }

    // Setter and Getter for ThreadManager and SpinnerPrefDlgFragment
    public void setDistCodeList(List<Opinet.DistrictCode> list) {
        distCodeList = list;
    }
    public List<Opinet.DistrictCode> getDistCodeList() {
        return distCodeList;
    }

    public void persistDistCode(String json) {
        persistString(json);
    }

}
