package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman.R;
import com.silverback.carman.databinding.MainContentStationsBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class StationRecyclerView extends CoordinatorLayout {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationRecyclerView.class);

    // Objects
    public MainContentStationsBinding childBinding;
    private WeakReference<View> mThisView;
    private int mTextViewResId;
    private int mRecyclerViewResId;

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

    public void getAttributes(Context context, AttributeSet attrs) {
        LayoutInflater inflater = LayoutInflater.from(context);
        childBinding = MainContentStationsBinding.inflate(inflater, this, true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        childBinding.recyclerviewStations.setHasFixedSize(true);
        childBinding.recyclerviewStations.setLayoutManager(layoutManager);

        TextView tvMessage = childBinding.tvNoStations;
        mTextViewResId = tvMessage.getId();
        log.i("textview id: %s", mTextViewResId);

        TypedArray typedArray = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.StationRecyclerView, 0, 0);
        try {
            mRecyclerViewResId = typedArray.getResourceId(R.styleable.StationRecyclerView_progressbar, -1);
            mTextViewResId = typedArray.getResourceId(R.styleable.StationRecyclerView_textview, -2);
            log.i("custom view:%s, %s", mRecyclerViewResId, mTextViewResId);
        } finally {
            typedArray.recycle();
        }
    }

    // This callback is invoked when the system attaches this view to a Window. It is invoked before
    // onDraw(), but may be invoked after onMeasure().
    @Override
    public void onAttachedToWindow() {
        // Always call the super method first
        super.onAttachedToWindow();
        /*
        if (mPBResId != -1 && getParent() instanceof View) {
            // Gets a handle to the sibling View
            View localView = ((View)getParent()).findViewById(mPBResId);
            // If the sibling View contains something, make it the weak reference for this View
            if (localView != null) mThisView = new WeakReference<>(localView);
        }

         */
    }


    // This callback is invoked when the view is removed from a Window. It "unsets" variables
    // to prevent memory leaks.
    @Override
    public void onDetachedFromWindow() {
        // If this View still exists, clears the weak reference, then sets the reference to null.
        if(mThisView != null) {
            mThisView.clear();
            mThisView = null;
        }
        //if(stationListTask != null) stationListTask = null;
        // Always call the super method last
        super.onDetachedFromWindow();
    }

    // Invoked from the parent GeneralFragment which is notified of successfully having a station
    // list completed.
    public void showStationListRecyclerView() {
        mThisView = new WeakReference<>(this);
        View localView = mThisView.get();
        if(localView != null) localView.setVisibility(View.VISIBLE);
    }

    // Invoked from the parent GeneralFragment when StationListTask failed to fetch any station
    // within a givene radius or the network connection failed to make.
    public void showTextView(SpannableString message){
        log.i("TextView ID: %s", mTextViewResId);
        //if(mTextViewResId != -2 && getParent() instanceof View) {
            childBinding.recyclerviewStations.setVisibility(View.GONE);
            View localView = childBinding.tvNoStations;
            mThisView = new WeakReference<>(localView);

            ((TextView)mThisView.get()).setText(message, TextView.BufferType.SPANNABLE);
            ((TextView)mThisView.get()).setMovementMethod(LinkMovementMethod.getInstance());
        //}


        /*
        if((mTextViewResId != -2) && getParent() instanceof View) {
            TextView localView = ((View)getParent()).findViewById(mTextViewResId);
            // If the sibling View contains something, make it the weak reference for this View
            if(localView != null) {
                //((View)getParent()).findViewById(mPBResId).setVisibility(View.GONE);
                // Hide RecyclerView if no stations failed to fetch in order for ClickableSpan to work.
                // Otherwise, RecyclerView covers the TextView which causes ClickSpannable to be untouchable.
                this.setVisibility(View.GONE);
                mThisView = new WeakReference<>(localView);
                ((TextView)mThisView.get()).setText(message, TextView.BufferType.SPANNABLE);
                ((TextView)mThisView.get()).setMovementMethod(LinkMovementMethod.getInstance());

            }
        }

         */
    }
}
