package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.PagerAdapterViewModel;
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
 * to be slow such that it is preferable to load sequentially. TabPagerTask is intiated first to
 * intantiate ExpTabPagerAdapter which creates the fragments to hold. On completing the task, the
 * tab-snyced viewpager is set up with the adapter and synced with the tab. Then, create the top
 * viewpager and the adapter to have the recent expenses and start animating the tab and the viewpager
 * at the top.
 *
 * At the same time, LocationTask is initiated to have the current location, with which GeneralFragment
 * starts StationListTask to fetch the current station via StationListViewModel when the task completes.
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
    private FragmentSharedModel fragmentSharedModel;
    private PagerAdapterViewModel pagerModel;

    private ExpTabPagerAdapter tabPagerAdapter;
    private ExpRecentPagerAdapter recentPagerAdapter;

    private ThreadTask tabPagerTask;
    private ThreadTask locationTask;

    // UIs
    private AppBarLayout appBar;
    private TabLayout expTabLayout;
    private FrameLayout topFrame;


    // Fields
    private int mPosition = 0;
    private int category;
    private boolean isTabVisible = false;
    private String pageTitle;
    private boolean isGeofencing;
    private String jsonSvcItems, jsonDistrict;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        if(getIntent().getAction() != null) {
            String action = getIntent().getAction();
            if(action.equals(Constants.NOTI_GEOFENCE)) {
                category = getIntent().getIntExtra(Constants.GEO_CATEGORY, -1);
                log.i("Category of notification: %s", category);
                isGeofencing = true;
            }
        }

        // Define ViewModels. ViewModelProviders.of(this) is deprecated. Instead, use ViewModelProvider
        // (requrieActivity()).  ViewModelProviders.of(this) is deprecated as well.
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        fragmentSharedModel = new ViewModelProvider(this).get(FragmentSharedModel.class);
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
        jsonSvcItems = mSettings.getString(Constants.SERVICE_ITEMS, null);
        jsonDistrict = mSettings.getString(Constants.DISTRICT, null);


        // LiveData observer of PagerAdapterViewModel to listen to whether ExpTabPagerAdapter has
        // finished to instantiate the fragments to display, then launch LocationTask to have
        // any near station within MIN_RADIUS, if any.
        pagerModel.getPagerAdapter().observe(this, adapter -> {
            tabPagerAdapter = adapter;
            tabPager.setAdapter(tabPagerAdapter);

            log.i("page: %s", mPosition);
            tabPager.setCurrentItem(mPosition);
            expTabLayout.setupWithViewPager(tabPager);

            // Create ViewPager for the last 5 recent expense statements in the top frame.
            // Required to use FrameLayout.addView() b/c StatFragment should be applied as a fragment,
            // not ViewPager. The viewpager is set to the apdater inanimSlideTabLayout for preventing
            // the animation from slowing down.
            expensePager = new ExpenseViewPager(this);
            expensePager.setId(View.generateViewId());
            recentPagerAdapter = new ExpRecentPagerAdapter(getSupportFragmentManager());

            addTabIconAndTitle(this, expTabLayout);
            animSlideTabLayout();


        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Start the thread to create ExpTabpagerAdapter and pass arguments to GasManagerFragment
        // and ServiceManagerFragment respectively and, at the same time, convert JSONServiceItems
        // to JSONArray in advance for the recyclerview in ServiceManagerj
        tabPagerTask = ThreadManager.startTabPagerTask(this, getSupportFragmentManager(), pagerModel,
                getDefaultParams(), jsonDistrict, jsonSvcItems);

        // On finishing TabPagerTask, set the ExpRecentPagerAdapter to ExpenseViewPager and
        // attach it in the top FrameLayout.
        locationTask = ThreadManager.fetchLocationTask(this, locationModel);
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
                Fragment fragment = tabPagerAdapter.getItem(mPosition);
                boolean isSaved = false;

                if(fragment instanceof GasManagerFragment) {
                    isSaved = ((GasManagerFragment) fragment).saveGasData();

                } else if(fragment instanceof ServiceManagerFragment) {
                    isSaved = ((ServiceManagerFragment) fragment).saveServiceData();
                }

                if(isSaved) {
                    finish();
                    return true;
                } else return false;
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
                mPosition = 0;
                pageTitle = getString(R.string.exp_title_gas);
                topFrame.addView(expensePager);
                break;

            case 1:
                mPosition = 1;
                pageTitle = getString(R.string.exp_title_service);
                topFrame.addView(expensePager);
                break;

            case 2:
                mPosition = 2;
                pageTitle = getString(R.string.exp_title_stat);
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


    // Animate TabLayout and the tap-syned viewpager sequentially, then as the animation completes,
    // the top viewpager is set up with ExpRecntPagerAdapter and add the viewpager to the frame.
    private void animSlideTabLayout() {
        float toolbarHeight = getActionbarHeight();
        float tabEndValue = (!isTabVisible)? toolbarHeight : 0;

        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator slideTab = ObjectAnimator.ofFloat(expTabLayout, "y", tabEndValue);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(topFrame, "translationY", tabEndValue);
        slideTab.setDuration(1000);
        slideViewPager.setDuration(1000);

        animSet.play(slideTab).before(slideViewPager);
        animSet.addListener(new AnimatorListenerAdapter(){
            public void onAnimationEnd(Animator animator) {
                expensePager.setAdapter(recentPagerAdapter);
                expensePager.setCurrentItem(0);
                topFrame.removeAllViews();
                topFrame.addView(expensePager);

                isTabVisible = !isTabVisible;
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
    public FragmentSharedModel getFragmentSharedModel() { return fragmentSharedModel; }
    public PagerAdapterViewModel getPagerModel() { return pagerModel; }

}
