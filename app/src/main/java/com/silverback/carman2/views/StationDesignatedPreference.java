package com.silverback.carman2.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class StationDesignatedPreference extends DialogPreference {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationDesignatedPreference.class);

    // Constructor
    public StationDesignatedPreference(Context context) {
        super(context);
    }
    public StationDesignatedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public StationDesignatedPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getAttributes(context, attrs);
    }
    public StationDesignatedPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getAttributes(context, attrs);
    }

    private void getAttributes(Context context, AttributeSet attrs) {

    }
}
