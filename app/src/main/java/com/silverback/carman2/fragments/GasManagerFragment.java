package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpenseViewPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.CustomPagerIndicator;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

/**
 * A simple {@link Fragment} subclass.
 */
public class GasManagerFragment extends Fragment implements
        ViewPager.OnPageChangeListener,
        NestedScrollView.OnScrollChangeListener, NestedScrollView.OnTouchListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Constants
    private static final int NumOfPages = 5;

    // Objects
    private TabLayout tabLayout;
    private NestedScrollView nestedScrollView;
    private ExpenseViewPagerAdapter viewPagerAdapter;
    private CustomPagerIndicator indicator;

    // Fields

    public GasManagerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(getActivity() != null) {
            tabLayout = getActivity().findViewById(R.id.tabLayout);

        }
        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_gas, container, false);

        NestedScrollView nestedScrollView = localView.findViewById(R.id.nestedScrollView);
        ViewPager viewPager = localView.findViewById(R.id.viewPager);
        indicator = localView.findViewById(R.id.indicator);

        nestedScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener(){
            @Override
            public void onScrollChanged() {
                log.i("Scroll Change");
            }
        });

        ExpenseViewPagerAdapter adapter = new ExpenseViewPagerAdapter(getFragmentManager(), NumOfPages);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
        viewPager.addOnPageChangeListener(this);

        indicator.createPanel(NumOfPages, R.drawable.dot_small, R.drawable.dot_large);

        return localView;
    }

    @Override
    public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        log.i("NestedScrollView.OnScrollChangeListener");

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        log.i("onToucn");
        return false;
    }


    // The following 3 overridingmethods are invoked by ViewPager.OnPageChangeListener
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }
    @Override
    public void onPageSelected(int position) {
        log.i("position: %s", position);
        indicator.selectDot(position);

    }
    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
