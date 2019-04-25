package com.silverback.carman2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.ViewPager;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.CustomPagerIndicator;

public class ExpensePagerView extends LinearLayout {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpensePagerView.class);
    // Constants
    private static final int NumPages = 5;

    // Objects
    private ViewPager pager;


    public ExpensePagerView(Context context) {
        super(context);
        getAttributes(context, null);
    }

    public ExpensePagerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }


    public ExpensePagerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }


    private void getAttributes(Context context, AttributeSet attrs) {

        LayoutInflater.from(context).inflate(R.layout.view_pager_expense, this, true);
        pager = findViewById(R.id.viewPager_expense);
        final CustomPagerIndicator indicator = findViewById(R.id.indicator);
        indicator.createPanel(NumPages, R.drawable.dot_small, R.drawable.dot_large);

        // ViewPager.OnPageChangeListener for animating the dots according to its state using
        // CustomPagerIndicator instance.
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                log.i("onPageScrolled");
            }

            @Override
            public void onPageSelected(int position) {
                log.i("onPageSelected");
                indicator.selectDot(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                log.i("onPageScrollStateChanged");
            }
        });
    }

    public void showExpensePagerView(ExpensePagerAdapter adapter) {
        pager.setAdapter(adapter);
        pager.setCurrentItem(0);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

}
