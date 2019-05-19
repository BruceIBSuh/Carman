package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.CarmanFragmentPagerAdapter;
import com.silverback.carman2.adapters.ExpensePagerAdapter;
import com.silverback.carman2.fragments.ExpenseFragment;
import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.StatGraphFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.views.ExpenseViewPager;

public class ExpenseActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int TAB_CARMAN = 1;
    private static final int TAB_BOARD = 2;
    private static final int NumOfPages = 5;

    // Objects
    private ExpenseFragment expenseFragment;
    private StatGraphFragment statGraphFragment;
    private ExpenseViewPager expensePager;
    private FragmentPagerAdapter pagerAdapter;
    private AppBarLayout appBar;
    private TabLayout tabLayout;
    private FrameLayout topFrame;

    // Fields
    private int currentPage = 0;
    private boolean isTabVisible = false;
    private String pageTitle;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        appBar = findViewById(R.id.appBar);
        appBar.addOnOffsetChangedListener(this);

        // Set Toolbar as Actionbar;
        Toolbar toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        FrameLayout frameFragments = findViewById(R.id.frame_expense_fragment);
        topFrame = findViewById(R.id.frame_top);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.exp_toolbar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewPager tabPager = new ViewPager(this);
        tabPager.setId(View.generateViewId());
        frameFragments.addView(tabPager);


        pagerAdapter = new CarmanFragmentPagerAdapter(getSupportFragmentManager());
        tabPager.setAdapter(pagerAdapter);
        tabPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(tabPager);

        addTabIconAndTitle(this, tabLayout);
        animSlideTabLayout();

        // Add a fragment to FrameLayout(topFrame)
        expensePager = new ExpenseViewPager(this);
        expensePager.setId(View.generateViewId());
        expensePager.initPager(getSupportFragmentManager());
        topFrame.addView(expensePager);


    }



    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume(){
        super.onResume();

        String title = mSettings.getString(Constants.VEHICLE_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1000, Menu.NONE, "SAVE");
        MenuItem item = menu.findItem(1000);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(R.drawable.ic_toolbar_save);
        return super.onCreateOptionsMenu(menu);
    }

    // Home button in Toolbar event handler, saving data in the current fragment
    // in SQLite
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home) {
            Fragment fragment = pagerAdapter.getItem(currentPage);
            switch(currentPage) {
                case 0 : // GasManagerFragment
                   ((GasManagerFragment)fragment).saveData();
                   break;
                case 1: // ServiceFragment
                   //((ServiceFragment)fragment).saveData();
                   break;
            }

            finish();
            return true;

        } else {
            log.i("SAVE button clicked");
        }

        return super.onOptionsItemSelected(item);
    }

    // The following 3 overriding methods are invoked by ViewPager.OnPageChangeListener.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        log.i("onPageScrolled: %s", position);
    }
    @Override
    public void onPageSelected(int position) {
        topFrame.removeAllViews();
        expensePager.setCurrentItem(0);

        switch(position) {
            case 0:
                pageTitle = "GasManager";
                topFrame.addView(expensePager);
                break;

            case 1:
                pageTitle = "ServiceManager";
                topFrame.addView(expensePager);
                break;

            case 2:
                pageTitle = "Statistics";
                statGraphFragment = new StatGraphFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_top, statGraphFragment).commit();
                break;
        }

    }
    @Override
    public void onPageScrollStateChanged(int state) {
        log.i("onPageScrollStateChanged");
    }

    // Invoked by AppBarLayout.OnOffsetChangeListener to be informed whether the scroll is located
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        log.i("AppBar scrolling state: %s", i);
        log.i("AppBar Total Scroll Range: %s", appBar.getTotalScrollRange());
        if(Math.abs(i) == appBar.getTotalScrollRange()) {
            //getSupportActionBar().setTitle(pageTitle);
        }

        //setBackgroundOpacity(appBar.getTotalScrollRange(), i);
    }



    /*
    // Prgramatically, add titles and icons on the TabLayout, which must be invoked after
    // setupWithViewPager when it is linked to ViewPager.
    @SuppressWarnings("ConstantConditions")
    private int addTabIconAndTitle(int tab) {

        //if(!tabTitleList.isEmpty()) tabTitleList.clear();
        //if(!tabIconList.isEmpty()) tabIconList.clear();
        switch(tab) {
            case TAB_CARMAN:
                tabTitleList = Arrays.asList(getResources().getStringArray(R.array.tab_carman_title));
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
    */



    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {
        float toolbarHeight = getActionbarHeight();
        float tabEndValue = (!isTabVisible)? toolbarHeight : 0;

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(topFrame, "translationY", tabEndValue);
        slideTab.setDuration(1000);
        slideViewPager.setDuration(1000);
        slideTab.start();
        slideViewPager.start();

        isTabVisible = !isTabVisible;

    }
    // Measures the size of an android attribute based on ?attr/actionBarSize
    /*
    private float getActionbarHeight() {
        TypedValue typedValue = new TypedValue();
        if(getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimension(typedValue.data, getResources().getDisplayMetrics());
        }
        return -1;
    }
    */

    private void setBackgroundOpacity(int maxRange, int scroll) {
        float bgAlpha = (float)((100 + (scroll * 100 / maxRange)) * 0.01);
        topFrame.setAlpha(bgAlpha);
    }


}
