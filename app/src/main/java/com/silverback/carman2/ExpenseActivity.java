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
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.ExpRecentPagerAdapter;
import com.silverback.carman2.adapters.ExpTabPagerAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceManagerFragment;
import com.silverback.carman2.fragments.StatGraphFragment;
import com.silverback.carman2.fragments.StatStmtsFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.PagerAdapterViewModel;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.ServiceRecyclerTask;
import com.silverback.carman2.threads.TabPagerTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.views.ExpenseViewPager;

public class ExpenseActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int MENU_ITEM_ID = 100;

    // Objects
    //private CarmanDatabase mDB;
    private ViewPager tabPager;
    private PagerAdapterViewModel pagerAdapterViewModel;
    private TabPagerTask tabPagerTask;
    private ExpTabPagerAdapter tabPagerAdapter;
    private ExpenseViewPager expensePager;
    private ExpRecentPagerAdapter recentPagerAdapter;
    private AppBarLayout appBar;
    private TabLayout expTabLayout;
    private FrameLayout topFrame;


    private LocationTask locationTask;
    private ServiceRecyclerTask serviceRecyclerTask;

    // Fields
    private boolean isFirst = true;
    private int currentPage = 0;
    private boolean isTabVisible = false;
    //private boolean isLocationTask = false;
    private String pageTitle;
    private String jsonServiceItems;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        appBar = findViewById(R.id.appBar);
        Toolbar toolbar = findViewById(R.id.toolbar_expense);
        expTabLayout = findViewById(R.id.tab_expense);
        topFrame = findViewById(R.id.frame_top_fragments);
        tabPager = findViewById(R.id.tabpager);
        // Set the toolbar as the working action bar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        appBar.addOnOffsetChangedListener(this);
        tabPager.addOnPageChangeListener(this);
        pageTitle = getString(R.string.exp_title_gas); //default title when the appbar scrolls up.


        //mDB = CarmanDatabase.getDatabaseInstance(getApplicationContext());
        LocationViewModel locationModel = ViewModelProviders.of(this).get(LocationViewModel.class);
        pagerAdapterViewModel = ViewModelProviders.of(this).get(PagerAdapterViewModel.class);
        jsonServiceItems = mSettings.getString(Constants.SERVICE_ITEMS, null);

        // Instantiate LocationTask to get the currentLocation, which is passed back to GasManagerFragment
        // via LocationViewModel
        locationTask = ThreadManager.fetchLocationTask(this, locationModel);

        // Create ViewPager to hold the tab fragments and add it in FrameLayout
        tabPagerTask = ThreadManager.startViewPagerTask(
                pagerAdapterViewModel, getSupportFragmentManager(), getDefaultParams());

        // Create ViewPager for last 5 recent expense statements in the top frame.
        // Required to use FrameLayout.addView() b/c StatFragment should be applied as a fragment,
        // not ViewPager.
        expensePager = new ExpenseViewPager(this);
        expensePager.setId(View.generateViewId());
        recentPagerAdapter = new ExpRecentPagerAdapter(getSupportFragmentManager());

        // LiveData observer of PagerAdapterViewModel to listen to whether ExpTabPagerAdapter has
        // finished to instantiate the fragments to display, then lauch LocationTask to have
        // any near station within MIN_RADIUS, if any.
        pagerAdapterViewModel.getPagerAdapter().observe(this, adapter -> {
            log.i("PagerAdapterViewModel: %s", adapter.getItem(0));
            tabPagerAdapter = adapter;
            tabPager.setAdapter(tabPagerAdapter);
            expTabLayout.setupWithViewPager(tabPager);
            addTabIconAndTitle(this, expTabLayout);
            animSlideTabLayout();

            // On finishing TabPagerTask, set the ExpRecentPagerAdapter to ExpenseViewPager and
            // attach it in the top FrameLayout.
            expensePager.setAdapter(recentPagerAdapter);
            expensePager.setCurrentItem(0);
            topFrame.addView(expensePager);
        });

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume(){
        super.onResume();

        String title = mSettings.getString(Constants.VEHICLE_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (locationTask != null) locationTask = null;
        if (tabPagerTask != null) tabPagerTask = null;
        if (serviceRecyclerTask != null) serviceRecyclerTask = null;

        // Destroy the static CarmanDatabase instance.
        CarmanDatabase.destroyInstance();
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
        // for revoking the callback of onPageSelected when initiating.
        log.i("onPageScrolled: %s, %s, %s", position, positionOffset, positionOffsetPixels);
        /*
        if(isFirst && positionOffset == 0 && positionOffsetPixels == 0) {
            log.i("onPageScrolled at initiating");
            onPageSelected(0);
            isFirst = false;
        }
        */

    }

    @Override
    public void onPageSelected(int position) {
        log.i("onPageSelected: %s", position);
        topFrame.removeAllViews();

        switch(position) {
            case 0: // GasManagerFragment
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
        if(state != 0) return;

        switch(currentPage) {
            case 1:
                /*
                if(serviceRecyclerTask == null)
                    serviceRecyclerTask = ThreadManager.startServiceRecyclerTask(
                           this, pagerAdapterViewModel, jsonServiceItems);

                 */
                break;
            case 2:
                log.i("onPageScrolledChanged: StatFragment");
                StatStmtsFragment fragment = (StatStmtsFragment)tabPagerAdapter.getPagerFragments()[2];
                fragment.queryExpense();
                break;
        }
    }


    // AppBarLayout.OnOffsetChangeListener invokes this method
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int scroll) {
        appBar.post(() -> {
            if(Math.abs(scroll) == 0)
                getSupportActionBar().setTitle(getString(R.string.exp_toolbar_title));
            else if(Math.abs(scroll) == appBar.getTotalScrollRange())
                getSupportActionBar().setTitle(pageTitle);
        });
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

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(expTabLayout, "y", tabEndValue);
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
