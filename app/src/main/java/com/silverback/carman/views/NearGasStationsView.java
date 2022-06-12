package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainContentGasStationsBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class NearGasStationsView extends RelativeLayout {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(NearGasStationsView.class);

    // Objects
    public MainContentGasStationsBinding binding;
    private WeakReference<View> mThisView;
    private WeakReference<View> mFabView;
    private int mTextViewResId;
    private int mRecyclerViewResId;
    private int mFabResId;

    // Default constructors
    public NearGasStationsView(Context context) {
        super(context);
    }
    public NearGasStationsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }
    public NearGasStationsView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        getAttributes(context, attrs);
    }

    public void getAttributes(Context context, AttributeSet attrs) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = MainContentGasStationsBinding.inflate(inflater, this, true);
        binding.recyclerGasStations.setHasFixedSize(true);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.NearGasStationsView, 0, 0);
        try {
            mRecyclerViewResId = a.getResourceId(R.styleable.NearGasStationsView_recyclerview, -1);
            mTextViewResId = a.getResourceId(R.styleable.NearGasStationsView_textview, -2);
        } finally {
            a.recycle();
        }
    }

    // This callback is invoked when the system attaches this view to a Window. It is invoked before
    // onDraw(), but may be invoked after onMeasure().
    @Override
    public void onAttachedToWindow() {
        // Always call the super method first
        super.onAttachedToWindow();
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

        if(mFabView != null) {
            mFabView.clear();
            mFabView = null;
        }


        // Always call the super method last
        super.onDetachedFromWindow();
    }

    // Getter for passing the viewbinding to the parent view b/c the parent can't touch the viewbinding
    // of the custom view with it.
    public RecyclerView getRecyclerView() {
        return binding.recyclerGasStations;
    }

    // Invoked from the parent GeneralFragment which is notified of successfully having a station
    // list completed.
    public void showStationRecyclerView() {
        if(mRecyclerViewResId != -1 && getParent() instanceof View) {
            mThisView = new WeakReference<>(binding.recyclerGasStations);
            View localView = mThisView.get();
            if(localView != null) localView.setVisibility(View.VISIBLE);
        }
    }

    // Invoked from the parent GeneralFragment when StationGasTask failed to fetch any station
    // within a givene radius or the network connection failed to make.
    public void showSpannableTextView(SpannableString message){
        if(mTextViewResId != -1 && getParent() instanceof View) {
            binding.recyclerGasStations.setVisibility(View.GONE);
            mThisView = new WeakReference<>(binding.tvNoStation);
            ((TextView)mThisView.get()).setText(message, TextView.BufferType.SPANNABLE);
            ((TextView)mThisView.get()).setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
