package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.ExpenseTabPagerAdapter;
import com.silverback.carman2.adapters.ExpenseTopPagerAdapter;
import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceManagerFragment;
import com.silverback.carman2.fragments.StatGraphFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.views.ExpenseViewPager;

public class ExpenseActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int MENU_ITEM_ID = 100;

    // Objects
    //private LocationTask locationTask;
    private ExpenseViewPager expensePager;
    private ExpenseTabPagerAdapter tabPagerAdapter;
    private AppBarLayout appBar;
    private TabLayout tabLayout;
    private FrameLayout topFrame;

    // Fields
    private int currentPage = 0;
    private boolean isTabVisible = false;
    //private boolean isLocationTask = false;
    private String pageTitle;

    private Toolbar toolbar;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        appBar = findViewById(R.id.appBar);
        appBar.addOnOffsetChangedListener(this);

        // Set Toolbar as Actionbar;
        toolbar = findViewById(R.id.toolbar_expense);
        FrameLayout tabFrame = findViewById(R.id.frame_tab_fragments);
        tabLayout = findViewById(R.id.tabLayout);
        topFrame = findViewById(R.id.frame_top_fragments);

        // Set the toolbar as the working action bar
        setSupportActionBar(toolbar);
        //getSupportActionBar().setTitle(getString(R.string.exp_toolbar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pageTitle = getString(R.string.exp_title_gas); //default title when the appbar scrolls up.

        // Create ViewPager to hold the tab fragments and add it in FrameLayout
        /*
        ViewPager tabViewPager = new ViewPager(this);
        tabViewPager.setId(View.generateViewId());
        tabFrame.addView(tabViewPager);


        tabPagerAdapter = new ExpenseTabPagerAdapter(getSupportFragmentManager());
        tabViewPager.setAdapter(tabPagerAdapter);
        tabViewPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(tabViewPager);
        */

        // Get defaultParams first and reset the radius param to Conststants.MIN_RADIUS, passing
        // it to GasManagerFragment.
        String[] defaultParams = getDefaultParams();
        defaultParams[1] = Constants.MIN_RADIUS;
        Bundle args = new Bundle();
        args.putStringArray("defaultParams", defaultParams);
        tabPagerAdapter.getItem(0).setArguments(args);

        addTabIconAndTitle(this, tabLayout);
        animSlideTabLayout();

        // Create ViewPager for last 5 recent expense statements in the top frame.
        // Required to use FrameLayout.addView() b/c StatFragment should be applied here.
        /*
        expensePager = new ExpenseViewPager(this);
        expensePager.setId(View.generateViewId());
        ExpenseTopPagerAdapter topPagerAdapter = new ExpenseTopPagerAdapter(getSupportFragmentManager());
        expensePager.setAdapter(topPagerAdapter);
        expensePager.setCurrentItem(0);
        topFrame.addView(expensePager);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ITEM_ID, Menu.NONE, R.string.exp_menuitem_title_save);
        MenuItem item = menu.findItem(MENU_ITEM_ID);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(R.drawable.ic_toolbar_save);

        return super.onCreateOptionsMenu(menu);
    }

    // Home button in Toolbar event handler, saving data in the current fragment
    // in SQLite
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch(item.getItemId()) {

            case android.R.id.home:
                finish();
                return true;

            case MENU_ITEM_ID:
                Fragment fragment = tabPagerAdapter.getItem(currentPage);
                boolean isSaved = false;

                if(fragment instanceof GasManagerFragment) {
                    isSaved = ((GasManagerFragment) fragment).saveGasData();
                } else if(fragment instanceof ServiceManagerFragment) {
                    isSaved = ((ServiceManagerFragment) fragment).saveServiceData();
                }

                if(isSaved) {
                    finish();
                    return true;

                } else {
                    Toast.makeText(this, "Failed to save the data", Toast.LENGTH_SHORT).show();
                    return false;
                }

        }

        return super.onOptionsItemSelected(item);
    }

    // The following 3 overriding methods are invoked by ViewPager.OnPageChangeListener.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        log.i("onPageScrolled: %s, %s, %s", position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        log.i("onPageSelected: %s", position);
        topFrame.removeAllViews();
        expensePager.setCurrentItem(0);

        switch(position) {
            case 0:
                currentPage = 0;
                pageTitle = getString(R.string.exp_title_gas);
                topFrame.addView(expensePager);

                break;

            case 1:
                currentPage = 1;
                pageTitle = getString(R.string.exp_title_service);
                topFrame.addView(expensePager);

                break;

            case 2:
                currentPage = 2;
                pageTitle = getString(R.string.exp_title_stat);
                StatGraphFragment statGraphFragment = new StatGraphFragment();

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_top_fragments, statGraphFragment).commit();

                break;
        }

    }
    @Override
    public void onPageScrollStateChanged(int state) {
        log.i("onPageScrollStateChanged:%s", state);
    }


    // AppBarLayout.OnOffsetChangeListener invokes this method
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int scroll) {
        appBar.post(new Runnable(){
            @Override
            public void run() {
                if(Math.abs(scroll) == 0)
                    getSupportActionBar().setTitle(getString(R.string.exp_toolbar_title));
                else if(Math.abs(scroll) == appBar.getTotalScrollRange())
                    getSupportActionBar().setTitle(getString(R.string.exp_title_gas));

            }
        });


        /*
        if(Math.abs(scroll) == appBar.getTotalScrollRange()) {
            getSupportActionBar().setTitle(pageTitle);
        } else if(Math.abs(scroll) == 0) {
            getSupportActionBar().setTitle("My Garage");
        }
        */

        // Fade the topFrame accroding to the scrolling of the AppBarLayout
        //setBackgroundOpacity(appBar.getTotalScrollRange(), scroll); //fade the app
        /*
        float bgAlpha = (float)((100 + (scroll * 100 / appBar.getTotalScrollRange())) * 0.01);
        topFrame.setAlpha(bgAlpha);
        */
    }


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


    private void setBackgroundOpacity(int maxRange, int scroll) {
        float bgAlpha = (float)((100 + (scroll * 100 / maxRange)) * 0.01);
        topFrame.setAlpha(bgAlpha);
    }
    */

    // Custom method that fragments herein may refer to SharedPreferences inherited from BaseActivity.
    public SharedPreferences getSettings() {
        return mSettings;
    }
}
