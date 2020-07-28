package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.icu.util.Measure;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ExpenseViewPager extends ViewPager {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseViewPager.class);

    // Objects
    public ExpenseViewPager(Context context) {
        super(context);
    }
    public ExpenseViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }

    // Get Attributes
    private void getAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StatGraphView);
        try {

        } finally {
            // init() cannot get started until the color attrs are obtained.

            typedArray.recycle();
        }

    }

    /*
    public void initPager(FragmentManager fm) {
        ExpRecentPagerAdapter adapter = new ExpRecentPagerAdapter(fm);
        setAdapter(adapter);
        setCurrentItem(0);
    }
    */

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        boolean wrapHeight = (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST ||
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED);
        log.i("height mode: %s", wrapHeight);
        if(wrapHeight) {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            log.i("Measured: %s, %s", width, height);

            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);

            if(getChildCount() > 0) {
                View firstChild = getChildAt(0);
                firstChild.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
                height = firstChild.getMeasuredHeight();
            }

            //height = (height == 0) ? 300 : height;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        }


        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        /*
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        boolean wrapHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST;
        log.i("height mode: %s", wrapHeight);
        if(wrapHeight) {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();

            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            if(getChildCount() > 0) {
                View firstChild = getChildAt(0);
                firstChild.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
                height = firstChild.getMeasuredHeight();
            }

            log.i("measured: %s, %s", width, height);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);

        int mode = MeasureSpec.getMode(heightMeasureSpec);
        log.i("measure: %s, %s, %s", widthMeasureSpec, heightMeasureSpec, mode);
        if(mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
            log.i("mode: %s", mode);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int height = 0; // working as minHeight;
            for(int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                log.i("child: %s", child);
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int h = child.getMeasuredHeight();
                if(h > height) height = h;
            }

            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        log.i("viewpager child: %s", getChildCount());

         */
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
