package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class StationRecyclerView extends RecyclerView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationRecyclerView.class);

    // UI's
    private WeakReference<View> mThisView;
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

    protected void getAttributes(Context context, AttributeSet attrs) {
        setHasFixedSize(true);
        LayoutManager layoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(layoutManager);

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
        if (mHideShowResId != -1 && getParent() instanceof View) {
            // Gets a handle to the sibling View
            View progBar = findViewById(mHideShowResId);
            // If the sibling View contains something, make it the weak reference for this View
            if (progBar != null) {
                mThisView = new WeakReference<>(progBar);
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


    public void showStationListRecyclerView() {
        mThisView = new WeakReference<View>(this);
        View localView = mThisView.get();

        if(localView != null) {
            ((View)getParent()).findViewById(mHideShowResId).setVisibility(View.GONE);
            localView.setVisibility(View.VISIBLE);
        }
    }

}
