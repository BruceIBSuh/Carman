package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.ExpRecentPagerAdapter;
import com.silverback.carman2.adapters.ExpTabPagerAdapter;
import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceManagerFragment;
import com.silverback.carman2.fragments.StatGraphFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.LocationViewModel;
import com.silverback.carman2.viewmodels.PagerAdapterViewModel;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.threads.ThreadTask;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.ExpenseViewPager;

/*
 * This activity is largely compolsed of two viewpagers. The viewpager which is synced w/ the tab
 * is at the bottom for holding such fragments as GasManagerFragment, ServiceManagerFragment, and
 * StatStmtsFragment. The other is at the top to hold PricePagerFragment w/ PricePagerAdapter which,
 * as a single fragment with multiple pages, shows the recent 5 expense statements of the first 2
 * tab-synced fragments and a single page of StatGraphFragment of the last tab-synced one, which
 * may extend to multi pages at a later time.
 *
 * Considerable components and resources to load at the same time may cause the top viewpager animation
 * to be slow such that it is preferable to load sequentially. ExpenseTabPagerTask is intiated first to
 * intantiate ExpTabPagerAdapter which creates the fragments to hold. On completing the task, the
 * tab-snyced viewpager is set up with the adapter and synced with the tab. Then, create the top
 * viewpager and the adapter to have the recent expenses and start animating the tab and the viewpager
 * at the top.
 *
 * At the same time, LocationTask is initiated to have the current location, with which GeneralFragment
 * starts StationListTask to fetch the current station via StationListViewModel when the task completes.
 *
 * On the other hand, separate process should be made if the activity gets started by tabbing the
 * geofence notification. In particular, be careful of the notification that contains the intent of
 * calling ServiceManagerFragment. Must add tabPager.setCurrentItem() when and only when the expense
 * viewpager is prepared. Otherwise, it makes an error b/c the expense view in ServiceManagerFragment
 * may be null.
 */

public class ExpenseActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int MENU_ITEM_ID = 1000;

    // Objects
    private ViewPager tabPager;
    private ExpenseViewPager expensePager;

    private LocationViewModel locationModel;
    private PagerAdapterViewModel pagerModel;

    private ExpTabPagerAdapter tabPagerAdapter;
    private ExpRecentPagerAdapter recentPagerAdapter;

    private ThreadTask tabPagerTask;
    private ThreadTask locationTask;

    // UIs
    private AppBarLayout appBar;
    private TabLayout expTabLayout;
    private FrameLayout topFrame;
    private MenuItem saveMenuItem;


    // Fields
    private int position = 0;
    private int category;
    //private boolean isTabVisible = false;
    private String pageTitle;
    private boolean isGeofencing;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        // Check if the activity gets started by tabbing the geofence notification.
        if(getIntent().getAction() != null) {
            if(getIntent().getAction().equals(Constants.NOTI_GEOFENCE)) {
                isGeofencing = true;
                category = getIntent().getIntExtra(Constants.GEO_CATEGORY, -1);
            }
        }

        // Define ViewModels. ViewModelProviders.of(this) is deprecated.
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        pagerModel = new ViewModelProvider(this).get(PagerAdapterViewModel.class);

        appBar = findViewById(R.id.appBar);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        expTabLayout = findViewById(R.id.tab_expense);
        topFrame = findViewById(R.id.frame_top_fragments);
        tabPager = findViewById(R.id.tabpager);

        // Set the toolbar as the working action bar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        appBar.addOnOffsetChangedListener(this);
        tabPager.addOnPageChangeListener(this);
        pageTitle = getString(R.string.exp_title_gas); //default title when the appbar scrolls up.

        expTabLayout.setupWithViewPager(tabPager);

        // Fetch the values from SharedPreferences
        String jsonSvcItems = mSettings.getString(Constants.SERVICE_ITEMS, null);
        String jsonDistrict = mSettings.getString(Constants.DISTRICT, null);

        // Create ExpTabPagerAdapter that containts GeneralFragment and ServiceFragment in the
        // background, trasferring params to each fragment and return the adapter.
        // Set the initial page number.
        position = 0;
        tabPagerTask = ThreadManager.startExpenseTabPagerTask(this, getSupportFragmentManager(), pagerModel,
                getDefaultParams(), jsonDistrict, jsonSvcItems);
        // callback when tabPagerTask finished.
        pagerModel.getPagerAdapter().observe(this, adapter -> {
            tabPagerAdapter = adapter;
            tabPager.setAdapter(tabPagerAdapter);
            tabPager.setCurrentItem(position);
            expTabLayout.setupWithViewPager(tabPager);

            // Create the viewpager to show the recent expenditure of gas or service. It should be
            // added to the framelayout because StatFragment cannot be applied in the same way as
            // in the other fragments.
            expensePager = new ExpenseViewPager(this);
            expensePager.setId(View.generateViewId());
            recentPagerAdapter = new ExpRecentPagerAdapter(getSupportFragmentManager());

            addTabIconAndTitle(this, expTabLayout);
            animSlideTabLayout();
        });

        // On finishing ExpenseTabPagerTask, set the ExpRecentPagerAdapter to ExpenseViewPager and
        // attach it in the top FrameLayout.
        locationTask = ThreadManager.fetchLocationTask(this, locationModel);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationTask != null) locationTask = null;
        if (tabPagerTask != null) tabPagerTask = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ITEM_ID, Menu.NONE, R.string.exp_menuitem_title_save);
        saveMenuItem = menu.findItem(MENU_ITEM_ID);
        saveMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        saveMenuItem.setIcon(R.drawable.ic_save_room);

        return super.onCreateOptionsMenu(menu);
    }

    // Home button in Toolbar event handler, saving data in the current fragment
    // in SQLite
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case android.R.id.home:
                log.i("isGeofencing: %s", isGeofencing);
                if(isGeofencing) {
                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.putExtra("isGeofencing", true);
                    startActivity(mainIntent);
                } else finish();

                return true;

            // menu for saving the gas or service data
            case MENU_ITEM_ID:
                Fragment fragment = tabPagerAdapter.getItem(position);
                boolean isSaved = false;
                if(position == Constants.GAS) isSaved = ((GasManagerFragment)fragment).saveGasData();
                else if(position == Constants.SVC) isSaved = ((ServiceManagerFragment)fragment).saveServiceData();
                if(isSaved) finish();
                return isSaved;

            default: return false;
        }

        //return super.onOptionsItemSelected(item);
    }

    // The following 3 overriding methods are invoked by ViewPager.OnPageChangeListener.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
        log.i("onPageSelected: %s", position);
        topFrame.removeAllViews();
        this.position = position;
        saveMenuItem.setVisible(true);

        switch(position) {
            case Constants.GAS: // GasManagerFragment
                pageTitle = getString(R.string.exp_title_gas);
                topFrame.addView(expensePager);
                break;

            case Constants.SVC:
                pageTitle = getString(R.string.exp_title_service);
                topFrame.addView(expensePager);
                break;

            case Constants.STAT:
                pageTitle = getString(R.string.exp_title_stat);
                saveMenuItem.setVisible(false);
                StatGraphFragment statGraphFragment = new StatGraphFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_top_fragments, statGraphFragment).commit();
                break;
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {}


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


    // Animate TabLayout and the tap-syned viewpager sequentially. As the animation completes,
    // the top viewpager is set up with ExpRecntPagerAdapter and add the viewpager to the frame and
    // start LocationTask.
    private void animSlideTabLayout() {
        float toolbarHeight = getActionbarHeight();

        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator slideTab = ObjectAnimator.ofFloat(expTabLayout, "y", toolbarHeight);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(topFrame, "translationY", toolbarHeight);
        slideTab.setDuration(1000);
        slideViewPager.setDuration(0);
        animSet.play(slideViewPager).before(slideTab);
        animSet.addListener(new AnimatorListenerAdapter(){
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                //isTabVisible = !isTabVisible;
                expensePager.setAdapter(recentPagerAdapter);
                expensePager.setCurrentItem(0);
                topFrame.removeAllViews();
                topFrame.addView(expensePager);

                // In case that this activity is started by the geofence notification, ServiceFragment
                // must be set to the current page only after the viewpager at the top has added to
                // the framelayout. Otherwise, an error occurs due to no child view in the viewpager.
                if(isGeofencing && category == Constants.SVC) tabPager.setCurrentItem(category);
            }
        });

        animSet.start();



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

    // Getter to be Referenced by the containing fragments
    public SharedPreferences getSettings() {
        return mSettings;
    }
    public LocationViewModel getLocationViewModel() { return locationModel; }
    public PagerAdapterViewModel getPagerModel() { return pagerModel; }

}
