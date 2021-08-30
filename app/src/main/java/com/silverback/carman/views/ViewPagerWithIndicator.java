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
        log.i("create ViewPagerWithIndicator");
        LayoutInflater inflater = LayoutInflater.from(context);

        binding = ViewPagerIndicatorBinding.inflate(inflater);
        viewpager = binding.pagerPrevExpense;
        tabLayout = binding.tabPrevExpense;
        log.i("view : %s, %s", viewpager, tabLayout);
    }

    public ViewPagerWithIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttributes(context, attrs);
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

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int leftPos = getPaddingLeft();
        int rightPos = right - left - getPaddingRight();

        final int parentTop = getPaddingTop();
        final int parentBottom = bottom - top - getPaddingBottom();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        log.i("child count: %s", count);
    }

    public ViewPager2 getViewPager() {
        return viewpager;
    }
}
