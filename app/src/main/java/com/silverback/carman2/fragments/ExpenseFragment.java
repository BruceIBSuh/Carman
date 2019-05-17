package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.CustomPagerIndicator;

/**
 * A simple {@link Fragment} subclass.
 * TEST class which should be deleted.
 *
 */
public class ExpenseFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseFragment.class);

    // Constants
    private static final int NumPages = 5;

    // Objects
    private ExpensePagerAdapter pagerAdapter;

    public ExpenseFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_recent_expense, container, false);

        ViewPager pager = localView.findViewById(R.id.viewpager);
        pagerAdapter = new ExpensePagerAdapter(getActivity().getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(0);

        CustomPagerIndicator indicator = localView.findViewById(R.id.indicator);
        indicator.createPanel(NumPages, R.drawable.dot_small, R.drawable.dot_large);
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

    public ExpensePagerAdapter getAdapter() {
        return pagerAdapter;
    }

}
