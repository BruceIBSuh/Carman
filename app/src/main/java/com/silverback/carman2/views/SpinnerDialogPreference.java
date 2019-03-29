package com.silverback.carman2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.preference.DialogPreference;

public class SpinnerDialogPreference extends DialogPreference  {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerDialogPreference.class);

    // Constants
    private final int mDialogLayoutResId = R.layout.dialogpref_spinner;

    // Objects
    private ArrayAdapter<CharSequence> sidoAdapter;

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

        // Create sidoAdapter using ArrayAdapter, and sigunAdapter using a BaseAdapter extending
        // cusom adapter.
        sidoAdapter = ArrayAdapter.createFromResource(
                context, R.array.sido_name, android.R.layout.simple_spinner_item);
        sidoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

}
