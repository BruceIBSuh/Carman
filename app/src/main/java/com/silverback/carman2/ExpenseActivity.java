package com.silverback.carman2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.CarmanFragmentPagerAdapter;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.utils.CustomPagerIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpenseActivity extends BaseActivity implements ViewPager.OnPageChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int TAB_CARMAN = 1;
    private static final int TAB_BOARD = 2;
    private static final int NumOfPages = 5;

    // Objects
    private GasManagerFragment gasFragment;
    private TabLayout tabLayout;
    private List<String> tabTitleList;
    private List<Drawable> tabIconList;
    private ViewPager tabPager, expensePager;
    private CustomPagerIndicator indicator;

    // Fields
    private boolean isTabVisible = false;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        // UIs
        Toolbar toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        indicator = findViewById(R.id.indicator);
        expensePager = findViewById(R.id.viewPager_expense);
        FrameLayout frameViewPager = findViewById(R.id.frameLayout_viewPager);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // ViewPager and Indicator
        //tabViewPager = findViewById(R.id.viewPager_tap);
        tabPager = new ViewPager(this);
        tabPager.setId(View.generateViewId());
        frameViewPager.addView(tabPager);

        FragmentPagerAdapter pagerAdapter = new CarmanFragmentPagerAdapter(getSupportFragmentManager());
        tabPager.setAdapter(pagerAdapter);
        tabPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(tabPager);

        // Custom method to set TabLayout title and icon, WHICH MUST BE INVOKED AFTER
        // TabLayout.setupWithViewPager as far as TabLayout links with ViewPager.
        tabTitleList = new ArrayList<>();
        tabIconList = new ArrayList<>();

        addTabIconAndTitle(TAB_CARMAN);
        animSlideTabLayout();

        // ViewPager to display receent 5 expenses on top of the screen.
        ExpensePagerAdapter expensePagerAdapter =
                new ExpensePagerAdapter(getSupportFragmentManager(), NumOfPages);
        expensePager.setAdapter(expensePagerAdapter);
        expensePager.setCurrentItem(0);
        indicator.createPanel(NumOfPages, R.drawable.dot_small, R.drawable.dot_large);

        /*
        expensePager = new ViewPager(this);
        expensePager.setId(View.generateViewId());
        ExpensePagerAdapter expenseAdapter = new ExpensePagerAdapter(getSupportFragmentManager(), NumOfPages);
        expensePager.setAdapter(expenseAdapter);
        expensePager.setCurrentItem(0);
        CustomPagerIndicator indicator = new CustomPagerIndicator(this);
        indicator.createPanel(NumOfPages, R.drawable.dot_small, R.drawable.dot_large);
        frameRecentExp.addView(expensePager);
        frameRecentExp.addView(indicator);
        */

    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume(){
        super.onResume();
        String title = mSettings.getString(Constants.VEHICLE_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                log.i("onOptionsItemSelected in GeneralSettingActivity");
                //NavUtils.navigateUpFromSameTask(this); not working b/c it might be a different task?
                //onBackPressed();
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
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
