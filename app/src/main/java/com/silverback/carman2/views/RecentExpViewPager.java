package com.silverback.carman2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.RecentExpPagerAdapter;
import com.silverback.carman2.utils.CustomPagerIndicator;

public class RecentExpViewPager extends ViewPager {

    private static final int NumPages = 5;

    public RecentExpViewPager(Context context) {
        super(context);
        getAttribute(context, null);
    }

    public RecentExpViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttribute(context, attrs);
    }

    private void getAttribute(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.fragment_expense_pager, this, true);
        ViewPager viewPager = findViewById(R.id.viewPager_expense);
        CustomPagerIndicator indicator = findViewById(R.id.indicator);
        /*
        RecentExpPagerAdapter adapter = new RecentExpPagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
        */
        indicator.createPanel(NumPages, R.drawable.dot_small, R.drawable.dot_large);
    }

}
