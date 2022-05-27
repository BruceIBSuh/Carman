package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.Preference;

import com.silverback.carman.R;

public class UserImagePreference extends Preference {

    public UserImagePreference(Context context) {
        super(context);
    }

    public UserImagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    public UserImagePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }


    protected void getAttributes(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.setting_pref_userimg);
        setIconSpaceReserved(true);
        setTitle("Setting UserImage");

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.UserImagePreference, 0, 0);
        try {
            //mShowIndicator = ta.getBoolean(R.styleable.ProgressBarPreference_showIndicator, false);
        } finally { ta.recycle();}
    }

    public void setPreferenceIcon() {

    }


}
