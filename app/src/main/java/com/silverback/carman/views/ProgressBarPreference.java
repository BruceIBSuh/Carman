package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.silverback.carman.R;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;


public class ProgressBarPreference extends Preference {

    private static final LoggingHelper log = LoggingHelperFactory.create(ProgressBarPreference.class);
    /*
    public ProgressBarPreference(Context context) {
        super(context);
    }
    */
    public ProgressBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    /*
    public ProgressBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }
     */

    protected void getAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressBarPreference);
        try {}
        finally { typedArray.recycle();}
    }

    public void showProgressBar(boolean isVisible) {
        if(isVisible) this.setEnabled(false);
        else this.setEnabled(true);

        // Set a wdiget at the right side of an preference
        setWidgetLayoutResource(isVisible? R.layout.view_pref_autodata : 0);
        notifyChanged();
    }

}
