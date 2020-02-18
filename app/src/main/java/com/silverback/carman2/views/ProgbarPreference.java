package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

/**
 * This class is a custom Prefererence which is used in the preference for setting the user image.
 * Setting the user image takes some time to upload Firebase Storage and ProgressBar needs to come
 * in to display the loading process. Not applied yet.
 */
public class ProgbarPreference extends Preference {

    private static final LoggingHelper log = LoggingHelperFactory.create(ProgbarPreference.class);

    private int progbarId;
    private int tvTitleId;

    // Constructors
    public ProgbarPreference(Context context) {
        super(context);
    }

    public ProgbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    public ProgbarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }


    protected void getAttributes(Context context, AttributeSet attrs) {

        setLayoutResource(R.layout.view_pref_progbar);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgbarPreference);
        try {
            tvTitleId = typedArray.getResourceId(R.styleable.ProgbarPreference_title, -1);
            progbarId = typedArray.getResourceId(R.styleable.ProgbarPreference_progbar, -1);
            log.i("Attributes: %s, %s", tvTitleId, progbarId);
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        log.i("holder: %s", holder);
        ImageView icon = (ImageView)holder.findViewById(R.id.icon_userImage);
        TextView tvTitle = (TextView)holder.findViewById(R.id.tv_title);
        ProgressBar progbar = (ProgressBar)holder.findViewById(R.id.pref_progbar);

    }

    @Override
    public void onAttached(){
        super.onAttached();
    }

    @Override
    public void onDetached(){
        super.onDetached();
    }


}
