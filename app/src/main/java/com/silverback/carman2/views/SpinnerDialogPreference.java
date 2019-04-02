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

    // Constants
    private final int mDialogLayoutResId = R.layout.dialogpref_spinner;

    // Objects
    private ArrayAdapter sidoAdapter;
    private DistrictSpinnerAdapter sigunAdapter;
    private List<Opinet.DistrictCode> distCodeList;
    private String jsonDistCode;

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

        } finally {
            typedArray.recycle();
        }
    }


    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

    // Abstrct method which PreferenceDialogFragmentCompat overrides
    public void onDialogClosed(boolean positiveResult){
        log.i("onDialogClosed: %s", positiveResult);
        if(positiveResult) persistString(jsonDistCode);
    }

    public void setJsonDistCode(String jsonString) {
        jsonDistCode = jsonString;
    }


}
