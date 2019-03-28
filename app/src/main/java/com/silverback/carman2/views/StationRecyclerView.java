package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.threads.StationTask;
import com.silverback.carman2.threads.ThreadManager;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class StationRecyclerView extends RecyclerView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationRecyclerView.class);


    // Objects
    private WeakReference<View> mThisView;
    private StationListAdapter mAdapter;
    private StationTask stationTask;
    private List<Opinet.GasStnParcelable> mStationList;
    private int mHideShowResId = -1;
    private int mTextViewResId = -2;


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
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(layoutManager);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StationRecyclerView);
        try {
            mHideShowResId = typedArray.getResourceId(R.styleable.StationRecyclerView_progressbar, -1);
            mTextViewResId = typedArray.getResourceId(R.styleable.StationRecyclerView_textview, -2);

        } finally {
            typedArray.recycle();
        }
    }

    public void initView(String[] defaults, Location location) {

        stationTask = ThreadManager.startStationListTask(this, defaults, location);
    }


    @Override
    protected void onAttachedToWindow() {
        // Always call the supermethod first
        super.onAttachedToWindow();
        if (mHideShowResId != -1 && (getParent() instanceof View)) {
            // Gets a handle to the sibling View
            View localView = ((View)getParent()).findViewById(mHideShowResId);
            // If the sibling View contains something, make it the weak reference for this View
            if (localView != null) {
                mThisView = new WeakReference<>(localView);
                log.d("mThisView: %s", this.mThisView);
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {}

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

        if(stationTask != null) stationTask = null;


        // Always call the super method last
        super.onDetachedFromWindow();
    }

    public void setNearStationList(List<Opinet.GasStnParcelable> stationList) {
        log.i("Download Station list: %s", stationList.size());
        mStationList = stationList;
        mAdapter = new StationListAdapter(mStationList);
        showStationListRecyclerView();
        setAdapter(mAdapter);
    }

    public void showStationListRecyclerView() {

        mThisView = new WeakReference<View>(this);
        View localView = mThisView.get();

        if(localView != null) {
            ((View)getParent()).findViewById(mHideShowResId).setVisibility(View.GONE);
            ((View)getParent()).findViewById(mTextViewResId).setVisibility(View.GONE);

            localView.setVisibility(View.VISIBLE);
        }
    }

    public void showTextView(String message){

        if((mTextViewResId != -2) && (getParent() instanceof View)) {

            View localView = ((View)getParent()).findViewById(mTextViewResId);
            // If the sibling View contains something, make it the weak reference for this View
            if (localView != null) {
                ((View)getParent()).findViewById(mHideShowResId).setVisibility(View.GONE);
                mThisView = new WeakReference<>(localView);
                ((TextView)mThisView.get()).setText(message);
            }
        }

    }

}
