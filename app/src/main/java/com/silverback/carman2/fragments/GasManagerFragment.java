package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.ExpenseViewPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.CustomPagerIndicator;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingChild;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

/**
 * A simple {@link Fragment} subclass.
 */
public class GasManagerFragment extends Fragment {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasManagerFragment.class);

    // Constants
    private static final int NumOfPages = 5;

    // Objects
    private TabLayout tabLayout;
    private ExpenseViewPagerAdapter viewPagerAdapter;
    private CustomPagerIndicator indicator;

    // Fields

    public GasManagerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_gas, container, false);

        // Create ViewPager and the custom indicator for paging.
        ViewPager viewPager = localView.findViewById(R.id.viewPager);
        indicator = localView.findViewById(R.id.indicator);
        NestedScrollView scroll = localView.findViewById(R.id.nestedScrollView);
        ExpenseViewPagerAdapter adapter = new ExpenseViewPagerAdapter(getFragmentManager(), NumOfPages);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        indicator.createPanel(NumOfPages, R.drawable.dot_small, R.drawable.dot_large);

        scroll.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(
                    NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                log.d("OnScrollChange");

            }
        });



        return localView;
    }

}
