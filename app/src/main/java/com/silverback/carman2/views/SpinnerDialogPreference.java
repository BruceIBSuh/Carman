package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import java.util.List;
import androidx.preference.DialogPreference;

public class SpinnerDialogPreference extends DialogPreference {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerDialogPreference.class);

    // Objects
    private String districtCode;

    /*
     *When you replace the 0 in the second constructor with R.attr.dialogPreferenceStyle
     * (For a DialogPreference) or R.attr.preferenceStyle (For any other preference) you
     * won’t face any design issues later. Thanks Ivan Soriano
     */

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

        setDialogLayoutResource(R.layout.dialogpref_spinner);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpinnerDialogPreference);
        try {

        } finally {
            typedArray.recycle();
        }
    }


    @Override
    public int getDialogLayoutResource() {
        return R.layout.dialogpref_spinner;
    }

}
