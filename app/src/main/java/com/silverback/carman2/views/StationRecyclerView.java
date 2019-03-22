package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class StationRecyclerView extends LinearLayout {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AvgPriceView.class);

    // UI's
    private LinearLayout linearLayout;
    private WeakReference<View> mThisView;
    private TextView tvWarning;

    // Fields
    private int mHideShowResId = -1;

    // Default constructors
    public StationRecyclerView(Context context) {
        super(context);
    }
    public StationRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public StationRecyclerView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }

    @SuppressWarnings("ConstantConditions")
    protected void getAttributes(Context context, AttributeSet attrs) {

        //LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //linearLayout = (LinearLayout)inflater.inflate(R.layout.view_recycler_stations, this, true);
        LayoutInflater.from(context).inflate(R.layout.view_recycler_stations, this, false);
        tvWarning = findViewById(R.id.tv_no_station);
        tvWarning.setText(R.string.general_no_station);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StationRecyclerView);

        try {
            mHideShowResId = typedArray.getResourceId(R.styleable.StationRecyclerView_progressbar, -1);
        } finally {
            typedArray.recycle();
        }
    }


    @Override
    protected void onAttachedToWindow() {
        // Always call the supermethod first
        super.onAttachedToWindow();

        if (mHideShowResId != -1) {
            // Gets a handle to the sibling View
            View localView = ((View) getParent()).findViewById(mHideShowResId);
            // If the sibling View contains something, make it the weak reference for this View
            if (localView != null) {
                mThisView = new WeakReference<>(localView);
                log.d("mThisView: %s", this.mThisView);
            }
        }
    }

    /*
     * This callback is invoked when the ImageView is removed from a Window. It "unsets" variables
     * to prevent memory leaks.
     */
    @Override
    protected void onDetachedFromWindow() {

        // If this View still exists, clears the weak reference, then sets the reference to null.
        if(mThisView != null) {
            mThisView.clear();
            mThisView = null;
        }

        //if(stationListTask != null) stationListTask = null;

        // Always call the super method last
        super.onDetachedFromWindow();
    }

}
