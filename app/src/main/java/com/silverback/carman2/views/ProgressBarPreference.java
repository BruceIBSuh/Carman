package com.silverback.carman2.views;

import android.content.Context;

import androidx.preference.Preference;

import com.silverback.carman2.R;

/**
 * This class is a custom Prefererence which is used in the preference for setting the user image.
 * Setting the user image takes some time to upload Firebase Storage and ProgressBar needs to come
 * in to display the loading process. Not applied yet.
 */
public class ProgressBarPreference extends Preference {

    public ProgressBarPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.view_pref_progbar);
    }
}
