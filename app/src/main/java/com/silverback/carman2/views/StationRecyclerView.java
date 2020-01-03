package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.location.Location;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.StationListViewModel;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;

import java.lang.ref.WeakReference;

public class StationRecyclerView extends RecyclerView {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationRecyclerView.class);

    // Objects
    private WeakReference<View> mThisView;
    private StationListTask stationListTask;
    private int mPBResId;
    private int mTextViewResId;
    private int mFabResId;
    private int state;

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

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        setHasFixedSize(true);
        setLayoutManager(layoutManager);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StationRecyclerView);
        try {
            mPBResId = typedArray.getResourceId(R.styleable.StationRecyclerView_progressbar, -1);
            mTextViewResId = typedArray.getResourceId(R.styleable.StationRecyclerView_textview, -2);
            mFabResId = typedArray.getResourceId(R.styleable.StationRecyclerView_fab, -3);

        } finally {
            typedArray.recycle();
        }
    }

    // This callback is invoked when the system attaches this view to a Window. It is invoked
    // before onDraw(), but may be invoked after onMeasure().
    @Override
    protected void onAttachedToWindow() {
        // Always call the supermethod first
        super.onAttachedToWindow();

        if (mPBResId != -1 && getParent() instanceof View) {
            // Gets a handle to the sibling View
            View localView = ((View)getParent()).findViewById(mPBResId);
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

        if(stationListTask != null) stationListTask = null;


        // Always call the super method last
        super.onDetachedFromWindow();
    }

    // Invoked from the parent GeneralFragment which is notified of successfully having a station
    // list completed.
    public void showStationListRecyclerView() {

        mThisView = new WeakReference<>(this);
        View localView = mThisView.get();

        if(localView != null) {
            ((View)getParent()).findViewById(mPBResId).setVisibility(View.GONE);
            ((View)getParent()).findViewById(mTextViewResId).setVisibility(View.GONE);
            ((View)getParent()).findViewById(mFabResId).setVisibility(View.VISIBLE);

            localView.setVisibility(View.VISIBLE);
        }
    }

    // Invoked from the parent GeneralFragment when StationListTask failed to fetch any station
    // within a givene radius or the network connection failed to make.
    public void showTextView(SpannableString message){

        if((mTextViewResId != -2) && getParent() instanceof View) {

            TextView localView = ((View)getParent()).findViewById(mTextViewResId);

            // If the sibling View contains something, make it the weak reference for this View
            if (localView != null) {

                ((View)getParent()).findViewById(mPBResId).setVisibility(View.GONE);
                // Hide RecyclerView if no stations failed to fetch in order for ClickableSpan to work.
                // Otherwise, RecyclerView covers the TextView which causes ClickSpannable to be untouchable.
                this.setVisibility(View.GONE);
                mThisView = new WeakReference<>(localView);

                ((TextView)mThisView.get()).setText(message, TextView.BufferType.SPANNABLE);
                ((TextView)mThisView.get()).setMovementMethod(LinkMovementMethod.getInstance());

            }
        }
    }

}
