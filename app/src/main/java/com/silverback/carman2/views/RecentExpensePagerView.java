package com.silverback.carman2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.ViewPager;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.utils.CustomPagerIndicator;

public class RecentExpensePagerView extends ConstraintLayout {

    // Logging

    // Constants
    private static final int NumPages = 5;

    // Objects
    private ViewPager pager;


    public RecentExpensePagerView(Context context) {
        super(context);
        getAttributes(context, null);
    }

    public RecentExpensePagerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }


    public RecentExpensePagerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttributes(context, attrs);
    }


    private void getAttributes(Context context, AttributeSet attrs) {

        LayoutInflater.from(context).inflate(R.layout.view_pager_recent_expense, this, true);
        pager = findViewById(R.id.viewPager_expense);
        CustomPagerIndicator indicator = findViewById(R.id.indicator);
        indicator.createPanel(NumPages, R.drawable.dot_small, R.drawable.dot_large);



        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecentExpensePagerView);
        try {

        } finally {
            typedArray.recycle();
        }
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
