package com.silverback.carman2.views;

import android.content.Context;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ExpenseViewPager extends ViewPager {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseViewPager.class);

    // Objects

    public ExpenseViewPager(Context context) {
        super(context);
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
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        if(mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int height = 120; // working as minHeight;
            for(int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int h = child.getMeasuredHeight();
                if(h > height) height = h;

            }
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }


}
