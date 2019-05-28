package com.silverback.carman2.fragments;


import android.animation.ObjectAnimator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFavoritePagerFragment extends Fragment {


    // Constants
    private static final String[] tabTitle = {"Gas Station", "Service Station"};

    public SettingFavoritePagerFragment() {
        // Required empty public constructor
    }

    // Objects
    private TabLayout tabLayout;
    // Fields
    private boolean isTabVisible = false;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_favorite_list, container, false);
        tabLayout = localView.findViewById(R.id.tabLayout);
        ViewPager viewPager = localView.findViewById(R.id.viewpager_favorite);

        tabLayout.addTab(tabLayout.newTab().setText("Gas Station"));
        tabLayout.addTab(tabLayout.newTab().setText("Service Station"));
        tabLayout.setupWithViewPager(viewPager);

        animSlideTabLayout();

        return localView;
    }

    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {

        TypedValue typedValue = new TypedValue();
        float toolbarHeight = 0f;

        if(getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            toolbarHeight = TypedValue.complexToDimension(typedValue.data, getResources().getDisplayMetrics());
        }
        float tabEndValue = (isTabVisible)? toolbarHeight : 0;


        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        //ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frameTop, "translationY", tabEndValue);
        slideTab.setDuration(1000);
        //slideViewPager.setDuration(1000);
        slideTab.start();
        //slideViewPager.start();

        isTabVisible = !isTabVisible;

    }


}
