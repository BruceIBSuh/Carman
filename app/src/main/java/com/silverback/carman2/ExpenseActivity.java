package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.ExpenseTabPagerAdapter;
import com.silverback.carman2.adapters.RecentExpensePagerAdapter;
import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceManagerFragment;
import com.silverback.carman2.fragments.StatGraphFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.StationListViewModel;
import com.silverback.carman2.models.AdapterViewModel;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.RecyclerAdapterTask;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.threads.ViewPagerTask;
import com.silverback.carman2.views.ExpenseViewPager;

public class ExpenseActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int MENU_ITEM_ID = 100;

    // Objects
    private Fragment targetFragment;
    private ViewPager tabPager;
    private AdapterViewModel adapterViewModel;
    private ViewPagerTask tabPagerTask;
    private ExpenseViewPager expensePager;
    private ExpenseTabPagerAdapter tabPagerAdapter;
    private AppBarLayout appBar;
    private TabLayout expenseTabLayout;
    private FrameLayout topFrame;


    private LocationTask locationTask;
    private LocationViewModel locationModel;
    private StationListViewModel stationModel;
    private Location location;
    private StationListTask stationListTask;

    private RecyclerAdapterTask recyclerTask;


    // UIs
    private EditText etStnName;
    private ProgressBar pbSearchStation;
    private ImageButton imgRefresh;

    // Fields
    private boolean isFirst = true;
    private int currentPage = 0;
    private boolean isTabVisible = false;
    //private boolean isLocationTask = false;
    private String pageTitle;
    private String jsonServiceItems;

    private Toolbar toolbar;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        locationModel = ViewModelProviders.of(this).get(LocationViewModel.class);
        adapterViewModel = ViewModelProviders.of(this).get(AdapterViewModel.class);
        jsonServiceItems = mSettings.getString(Constants.SERVICE_ITEMS, null);


        appBar = findViewById(R.id.appBar);
        appBar.addOnOffsetChangedListener(this);
        toolbar = findViewById(R.id.toolbar_expense);
        expenseTabLayout = findViewById(R.id.tab_expense);
        topFrame = findViewById(R.id.frame_top_fragments);
        tabPager = findViewById(R.id.tabpager);

        // Set the toolbar as the working action bar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tabPager.addOnPageChangeListener(this);
        pageTitle = getString(R.string.exp_title_gas); //default title when the appbar scrolls up.

        // Create ViewPager to hold the tab fragments and add it in FrameLayout
        tabPagerTask = ThreadManager.startViewPagerTask(
                adapterViewModel, getSupportFragmentManager(), getDefaultParams());
        /*
        tabPagerAdapter = new ExpenseTabPagerAdapter(getSupportFragmentManager());
        tabPager.setOffscreenPageLimit(0);
        tabPager.setAdapter(tabPagerAdapter);
        tabPager.addOnPageChangeListener(this);
        expenseTabLayout.setupWithViewPager(tabPager);
        // Get defaultParams first and reset the radius param to Conststants.MIN_RADIUS, passing
        // it to GasManagerFragment.
        String[] defaults = getDefaultParams();
        defaults[1] = Constants.MIN_RADIUS;
        Bundle args = new Bundle();
        args.putStringArray("defaultParams", defaults);
        tabPagerAdapter.getItem(0).setArguments(args);
        addTabIconAndTitle(this, expenseTabLayout);
        animSlideTabLayout();
        */

        // Create ViewPager for last 5 recent expense statements in the top frame.
        // Required to use FrameLayout.addView() b/c StatFragment should be applied as a fragment,
        // not ViewPager.
        expensePager = new ExpenseViewPager(this);
        expensePager.setId(View.generateViewId());
        RecentExpensePagerAdapter topPagerAdapter = new RecentExpensePagerAdapter(getSupportFragmentManager());
        expensePager.setAdapter(topPagerAdapter);
        expensePager.setCurrentItem(0);
        topFrame.addView(expensePager);

        // LiveData observer of AdapterViewModel to listen to whether ExpenseTabPagerAdapter has
        // finished to instantiate the fragments to display, then lauch LocationTask to have
        // any near station within MIN_RADIUS, if any.
        adapterViewModel.getPagerAdapter().observe(this, adapter -> {
            log.i("AdapterViewModel: %s", adapter.getItem(0));
            tabPagerAdapter = adapter;
            tabPager.setAdapter(tabPagerAdapter);
            expenseTabLayout.setupWithViewPager(tabPager);

            addTabIconAndTitle(this, expenseTabLayout);
            animSlideTabLayout();

            if(locationTask == null) locationTask = ThreadManager.fetchLocationTask(this, locationModel);
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
        if(tabPagerTask != null) tabPagerTask = null;
        if(locationTask != null) locationTask = null;
        if(recyclerTask != null) recyclerTask = null;
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
        if(isFirst && positionOffset == 0 && positionOffsetPixels == 0) {
            log.i("onPageScrolled at initiating");
            onPageSelected(0);
            isFirst = false;
        }

    }

    @Override
    public void onPageSelected(int position) {
        log.i("onPageSelected: %s", position);
        topFrame.removeAllViews();
        expensePager.setCurrentItem(0);

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
        switch (currentPage) {
            case 0:
                break;
            case 1:
                if(recyclerTask == null) {
                    log.i("RecyclerTask");
                    recyclerTask = ThreadManager.startRecyclerAdapterTask(adapterViewModel, jsonServiceItems);
                }

                break;
            case 2:
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

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(expenseTabLayout, "y", tabEndValue);
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
