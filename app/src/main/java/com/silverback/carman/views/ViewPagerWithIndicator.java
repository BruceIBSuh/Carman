package com.silverback.carman.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman.R;
import com.silverback.carman.databinding.ViewPagerIndicatorBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ViewPagerWithIndicator extends ViewGroup {

    private static final LoggingHelper log = LoggingHelperFactory.create(ViewPagerWithIndicator.class);

    private ViewPagerIndicatorBinding binding;
    private ViewPager2 viewpager;
    private TabLayout tabLayout;


    public ViewPagerWithIndicator(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = ViewPagerIndicatorBinding.inflate(inflater);
        viewpager = binding.pagerPrevExpense;
        tabLayout = binding.tabPrevExpense;
    }


    public ViewPagerWithIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
    }


    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        viewpager.layout(0,0, viewpager.getMeasuredWidth(), viewpager.getMeasuredHeight());
    }

    protected void getAttributes(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_pager_indicator, this, true);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ViewPagerWithIndicator);
        try {
            log.i("typedArray");
        } finally {
            typedArray.recycle();
        }
    }

    public ViewPager2 getViewPager() {
        return viewpager;
    }
}
