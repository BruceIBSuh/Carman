package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.CustomPagerIndicator;

/**
 *
 */
public class RecentExpFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(RecentExpFragment.class);

    // Constants
    private static final int NumPages = 5;

    // Objects

    // Constructor
    public RecentExpFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_recent_exp, container, false);

        ViewPager pager = localView.findViewById(R.id.viewPager_expense);
        final CustomPagerIndicator indicator = localView.findViewById(R.id.indicator);

        ExpensePagerAdapter adapter = new ExpensePagerAdapter(getFragmentManager(), NumPages);
        indicator.createPanel(NumPages, R.drawable.dot_small, R.drawable.dot_large);
        pager.setAdapter(adapter);

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

        return localView;
    }

}
