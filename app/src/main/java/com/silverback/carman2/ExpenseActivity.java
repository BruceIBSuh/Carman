package com.silverback.carman2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.CarmanFragmentPagerAdapter;
import com.silverback.carman2.adapters.ExpenseViewPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.CustomPagerIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpenseActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int TAB_CARMAN = 1;
    private static final int TAB_BOARD = 2;
    private static final int NumOfPages = 5;

    // Objects
    private TabLayout tabLayout;
    private List<String> tabTitleList;
    private List<Drawable> tabIconList;
    private ViewPager tabViewPager, expenseViewPager;
    private CustomPagerIndicator indicator;

    // Fields
    private boolean isTabVisible = false;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        Toolbar toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        indicator = findViewById(R.id.indicator);
        expenseViewPager = findViewById(R.id.viewPager_expense);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        // ViewPager and Indicator
        tabViewPager = new ViewPager(this);
        tabViewPager.setId(View.generateViewId());
        FragmentPagerAdapter pagerAdapter = new CarmanFragmentPagerAdapter(getSupportFragmentManager());
        tabViewPager.setAdapter(pagerAdapter);
        tabViewPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(tabViewPager);

        // Custom method to set TabLayout title and icon, WHICH MUST BE INVOKED AFTER
        // TabLayout.setupWithViewPager as far as TabLayout links with ViewPager.
        tabTitleList = new ArrayList<>();
        tabIconList = new ArrayList<>();

        // ViewPager to display receent 5 expenses on top of the screen.
        ExpenseViewPagerAdapter expenseAdapter =
                new ExpenseViewPagerAdapter(getSupportFragmentManager(), NumOfPages);
        expenseViewPager.setAdapter(expenseAdapter);
        expenseViewPager.setCurrentItem(0);
        indicator.createPanel(NumOfPages, R.drawable.dot_small, R.drawable.dot_large);


        addTabIconAndTitle(TAB_CARMAN);
        animSlideTabLayout();
    }


    // Prgramatically, add titles and icons on the TabLayout, which must be invoked after
    // setupWithViewPager when it is linked to ViewPager.
    @SuppressWarnings("ConstantConditions")
    private int addTabIconAndTitle(int tab) {

        //if(!tabTitleList.isEmpty()) tabTitleList.clear();
        //if(!tabIconList.isEmpty()) tabIconList.clear();

        switch(tab) {
            case TAB_CARMAN:
                tabTitleList = Arrays.asList(getResources().getStringArray(R.array.tap_carman_title));
                Drawable[] icons = {
                        getDrawable(R.drawable.ic_gas),
                        getDrawable(R.drawable.ic_service),
                        getDrawable(R.drawable.ic_stats)};

                tabIconList = Arrays.asList(icons);
                break;

            case TAB_BOARD:
                tabTitleList = Arrays.asList(getResources().getStringArray(R.array.tap_board_title));
                icons = new Drawable[]{};
                tabIconList = Arrays.asList(icons);
                break;

        }

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            log.i("Title: %s", tabTitleList.get(i));
            tabLayout.getTabAt(i).setText(tabTitleList.get(i));
            if(!tabIconList.isEmpty()) tabLayout.getTabAt(i).setIcon(tabIconList.get(i));
        }

        return tab;
    }

    // The following 3 overriding methods are invoked by ViewPager.OnPageChangeListener.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }
    @Override
    public void onPageSelected(int position) {

    }
    @Override
    public void onPageScrollStateChanged(int state) {

    }

    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {
        float toolbarHeight = getActionbarHeight();
        float tabEndValue = (!isTabVisible)? toolbarHeight : 0;

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        //ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frameLayout, "translationY", tabEndValue);
        slideTab.setDuration(1000);
        //slideViewPager.setDuration(1000);
        slideTab.start();
        //slideViewPager.start();

        isTabVisible = !isTabVisible;

    }


    // Measures the size of an android attribute based on ?attr/actionBarSize
    private float getActionbarHeight() {
        TypedValue typedValue = new TypedValue();
        if(getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimension(typedValue.data, getResources().getDisplayMetrics());
        }
        return -1;
    }
}
