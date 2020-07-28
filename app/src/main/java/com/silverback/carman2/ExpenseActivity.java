package com.silverback.carman2;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private ViewPager expensePager;
    private LocationViewModel locationModel;
    private PagerAdapterViewModel pagerModel;
    private ExpTabPagerAdapter tabPagerAdapter;

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

        tabPagerTask = ThreadManager.startExpenseTabPagerTask(this, getSupportFragmentManager(), pagerModel,
                getDefaultParams(), jsonDistrict, jsonSvcItems);

        // callback when tabPagerTask finished.
        pagerModel.getPagerAdapter().observe(this, adapter -> {
            position = 0;
            tabPagerAdapter = adapter;
            tabPager.setAdapter(tabPagerAdapter);
            tabPager.setCurrentItem(position);
            expTabLayout.setupWithViewPager(tabPager);

            // Create the tab and slide down the tab and the topFrame to offset the toolbar height.
            addTabIconAndTitle(this, expTabLayout);
            animSlideTabLayout();
        });

        // Create the viewpager for showing the recent expenses.
        expensePager = new ViewPager(this);
        expensePager.setId(View.generateViewId());
        ExpRecentPagerAdapter recentPagerAdapter = new ExpRecentPagerAdapter(getSupportFragmentManager());
        expensePager.setAdapter(recentPagerAdapter);

        // Consider this process should be behind the layout to lessen the ram load.
        locationTask = ThreadManager.fetchLocationTask(this, locationModel);
    }

    @Override
    public void onPause() {
        super.onPause();

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
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
    }

    // The following 3 overriding methods are invoked by ViewPager.OnPageChangeListener.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //log.i("onPageScrolled: %s, %s, %s", position, positionOffset, positionOffsetPixels);
    }

    int topFrameHeight = 0;
    @Override
    public void onPageSelected(int position) {
        topFrame.removeAllViews();
        this.position = position;
        saveMenuItem.setVisible(true);

        switch(position) {
            case Constants.GAS: // GasManagerFragment
                pageTitle = getString(R.string.exp_title_gas);
                //createExpenseViewPager(280);
                //topFrame.addView(expensePager);
                topFrameHeight = 280;
                break;

            case Constants.SVC:
                pageTitle = getString(R.string.exp_title_service);
                //createExpenseViewPager(220);
                //topFrame.addView(expensePager);
                topFrameHeight = 220;
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
    public void onPageScrollStateChanged(int state) {
        log.i("onPageScrollstateChanged: %s", state);
        if(position >= 0 && position <= 1 && state == 0) {
            createExpenseViewPager(topFrameHeight);
            topFrame.addView(expensePager);
        }
    }


    // AppBarLayout.OnOffsetChangeListener invokes this method
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int scroll) {
        appBar.post(() -> {
            if(Math.abs(scroll) == 0) getSupportActionBar().setTitle(getString(R.string.exp_toolbar_title));
            else if(Math.abs(scroll) == appBar.getTotalScrollRange()) getSupportActionBar().setTitle(pageTitle);
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
        final float toolbarHeight = getActionbarHeight();
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator slideTab = ObjectAnimator.ofFloat(expTabLayout, "translationY", toolbarHeight);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(topFrame, "translationY", toolbarHeight);
        slideTab.setDuration(1000);
        slideViewPager.setDuration(1000);
        animSet.play(slideTab).with(slideViewPager);
        animSet.addListener(new AnimatorListenerAdapter(){
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                if(topFrame.getChildCount() > 0) topFrame.removeAllViews();
                createExpenseViewPager(280);
                topFrame.addView(expensePager);
                // In case that this activity is started by the geofence notification, ServiceFragment
                // must be set to the current page only after the viewpager at the top has added to
                // the framelayout. Otherwise, an error occurs due to no child view in the viewpager.
                if(isGeofencing && category == Constants.SVC) tabPager.setCurrentItem(category);
            }
        });
        animSet.start();
    }

    private void createExpenseViewPager(int height) {
        ValueAnimator anim = ValueAnimator.ofInt(0, height);
        anim.addUpdateListener(valueAnimator -> {
            int value = (Integer)valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams params = topFrame.getLayoutParams();
            params.height = value;
            topFrame.setLayoutParams(params);
        });

        anim.setDuration(500);
        anim.start();

        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                //ExpRecentPagerAdapter recentPagerAdapter = new ExpRecentPagerAdapter(getSupportFragmentManager());
                //expensePager.setAdapter(recentPagerAdapter);
                expensePager.setCurrentItem(0);
            }
        });

        /*
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        expensePager.setLayoutParams(params);

        ExpRecentPagerAdapter recentPagerAdapter = new ExpRecentPagerAdapter(getSupportFragmentManager());
        expensePager.setAdapter(recentPagerAdapter);
        expensePager.setCurrentItem(0);

         */
    }

    // Getter to be Referenced by the containing fragments
    public LocationViewModel getLocationViewModel() { return locationModel; }
    public PagerAdapterViewModel getPagerModel() { return pagerModel; }

}
