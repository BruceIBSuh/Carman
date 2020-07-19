package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.Preference;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ProgressBarPreference extends Preference {

    private static final LoggingHelper log = LoggingHelperFactory.create(ProgressBarPreference.class);

    // Objects
    private ProgressBar pbAutoData;

    public ProgressBarPreference(Context context) {
        super(context);
    }
    public ProgressBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public ProgressBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }

    protected void getAttributes(Context context, AttributeSet attrs) {
        //this.setWidgetLayoutResource(R.layout.view_pref_autodata);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressBarPreference);
        try {

        } finally {
            typedArray.recycle();
        }
    }

    public void showProgressBar(boolean isVisible) {
        setWidgetLayoutResource(isVisible? R.layout.view_pref_autodata : 0);
        notifyChanged();
    }

}
