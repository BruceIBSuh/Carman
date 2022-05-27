package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;


public class AutoDataPreference extends Preference {

    private static final LoggingHelper log = LoggingHelperFactory.create(AutoDataPreference.class);
    private boolean mShowIndicator;
    /*
    public AutoDataPreference(Context context) {
        super(context);
    }
    */
    public AutoDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);

    }
    /*
    public AutoDataPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }
     */

    protected void getAttributes(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AutoDataPreference, 0, 0);
        try {
            mShowIndicator = ta.getBoolean(R.styleable.AutoDataPreference_showIndicator, false);
        } finally { ta.recycle();}
    }

    public void showProgressBar(boolean isVisible) {
        this.setEnabled(!isVisible);
        // Set a wdiget at the right side of an preference
        setWidgetLayoutResource(isVisible? R.layout.view_pref_autodata : 0);
        notifyChanged();
    }
}
