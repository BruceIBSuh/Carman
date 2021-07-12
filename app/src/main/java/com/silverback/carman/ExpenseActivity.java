package com.silverback.carman;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.silverback.carman.adapters.ExpRecentAdapter;
import com.silverback.carman.adapters.ExpTabAdapter;
import com.silverback.carman.databinding.ActivityExpenseBinding;
import com.silverback.carman.fragments.GasManagerFragment;
import com.silverback.carman.fragments.ServiceManagerFragment;
import com.silverback.carman.fragments.StatGraphFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.threads.ThreadTask;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.PagerAdapterViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.Objects;

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

public class ExpenseActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int MENU_ITEM_ID = 1000;

    // Objects
    private ActivityExpenseBinding binding;//ViewBinding instead of using findViewById()
    private LocationViewModel locationModel;
    private PagerAdapterViewModel pagerModel;
    private StationListViewModel stnListModel;
    private ExpTabAdapter expTabAdapter;

    private ThreadTask tabPagerTask;
    private ThreadTask locationTask;
    private ThreadTask StationListTask;

    private ViewPager2 pagerRecentExp;
    private MenuItem saveMenuItem;
    private ViewTreeObserver vto;

    // Fields
    private int position;
    private int prevHeight;
    private int category;
    private String pageTitle;
    private boolean isGeofencing;
    private Location mPrevLocation;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        stnListModel = new ViewModelProvider(this).get(StationListViewModel.class);

        // Set the toolbar as the working action bar
        setSupportActionBar(binding.toolbarExpense);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        binding.appBar.addOnOffsetChangedListener(this);
        pageTitle = getString(R.string.exp_title_gas); //default title when the appbar scrolls up.

        expTabAdapter = new ExpTabAdapter(getSupportFragmentManager(), getLifecycle());
        binding.pagerTabFragment.setAdapter(expTabAdapter);
        binding.pagerTabFragment.setCurrentItem(0);
        binding.pagerTabFragment.registerOnPageChangeCallback(getPageChageCallback());

        // Associate TabLayout with ViewPager2 using TabLayoutMediator.
        String[] titles = getResources().getStringArray(R.array.tab_carman_title);
        Drawable[] icons = {
                AppCompatResources.getDrawable(this, R.drawable.ic_gas),
                AppCompatResources.getDrawable(this, R.drawable.ic_service),
                AppCompatResources.getDrawable(this, R.drawable.ic_stats)
        };

        // A mediator to link TabLayout w/ ViewPager2. TabLayoutMediator listens to ViewPager2
        // OnPageChangeCallback, TabLayout OnTabSelectedListener and RecyclerView AdapterDataObserver.
        new TabLayoutMediator(binding.tabExpense, binding.pagerTabFragment, true, true, (tab, pos) -> {
            tab.setText(titles[pos]);
            tab.setIcon(icons[pos]);
            animSlideTabLayout();
        }).attach();


        // Fetch the values from SharedPreferences
        String jsonSvcItems = mSettings.getString(Constants.SERVICE_ITEMS, null);
        String jsonDistrict = mSettings.getString(Constants.DISTRICT, null);
        log.i("json data", jsonSvcItems);

        // Create ExpTabPagerAdapter that containts GasFragment and ServiceFragment in the
        // background, trasferring params to each fragment and return the adapter.
        // Set the initial page number.
        tabPagerTask = sThreadManager.startExpenseTabPagerTask(this, getSupportFragmentManager(),
                pagerModel, getDefaultParams(), jsonDistrict, jsonSvcItems);


        pagerRecentExp = new ViewPager2(this);
        pagerRecentExp.setId(View.generateViewId());
        ExpRecentAdapter recentAdapter = new ExpRecentAdapter(getSupportFragmentManager(), getLifecycle());
        //binding.pagerExpense.setAdapter(recentAdapter);
        pagerRecentExp.setAdapter(recentAdapter);


        // Consider this process should be behind the layout to lessen the ram load.
        if(!isGeofencing) {
            locationTask = sThreadManager.fetchLocationTask(this, locationModel);
            locationModel.getLocation().observe(this, location -> {
                if(mPrevLocation == null || location.distanceTo(mPrevLocation) > Constants.UPDATE_DISTANCE) {
                    log.i("current location for finding station:%s", location);
                    final String[] defaults = getDefaultParams();
                    defaults[1] = Constants.MIN_RADIUS;
                    StationListTask = sThreadManager.startStationListTask(stnListModel, location, defaults);
                    mPrevLocation = location;
                }
            });
        }
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
                Fragment fragment = expTabAdapter.createFragment(position);//getItem(position);
                boolean isSaved = false;
                if(position == Constants.GAS) isSaved = ((GasManagerFragment)fragment).saveGasData();
                else if(position == Constants.SVC) isSaved = ((ServiceManagerFragment)fragment).saveServiceData();
                if(isSaved) finish();
                return isSaved;
            default: return false;
        }
    }

    // AppBarLayout.OnOffsetChangeListener invokes this method
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int scroll) {
        binding.appBar.post(() -> {
            if(Math.abs(scroll) == 0) getSupportActionBar().setTitle(getString(R.string.exp_toolbar_title));
            else if(Math.abs(scroll) == binding.appBar.getTotalScrollRange()) getSupportActionBar().setTitle(pageTitle);
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
        ObjectAnimator slideTab = ObjectAnimator.ofFloat(binding.tabExpense, "translationY", toolbarHeight);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(binding.frameTopFragments, "translationY", toolbarHeight);
        slideTab.setDuration(1000);
        slideViewPager.setDuration(100);
        animSet.play(slideTab).with(slideViewPager);

        animSet.addListener(new AnimatorListenerAdapter(){
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                if(binding.frameTopFragments.getChildCount() > 0) binding.frameTopFragments.removeAllViews();

                animSlideTopFrame(0, 150);
                prevHeight = 150;

                binding.frameTopFragments.addView(pagerRecentExp);

                // In case that this activity is started by the geofence notification, ServiceFragment
                // must be set to the current page only after the viewpager at the top has added to
                // the framelayout. Otherwise, an error occurs due to no child view in the viewpager.
                if(isGeofencing && category == Constants.SVC) binding.pagerTabFragment.setCurrentItem(category);
            }
        });
        animSet.start();
    }

    private void animSlideTopFrame(int oldY, int newY) {
        log.i("animate top frame");
        // Convert the dp unit to pixels
        int prevHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                oldY, getResources().getDisplayMetrics());
        int newHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newY, getResources().getDisplayMetrics());

        // Animate to slide the top frame down to the measured height.
        ValueAnimator anim = ValueAnimator.ofInt(prevHeight, newHeight);
        ViewGroup.LayoutParams params = binding.frameTopFragments.getLayoutParams();

        anim.addUpdateListener(valueAnimator -> {
            params.height = (Integer)valueAnimator.getAnimatedValue();;
            binding.frameTopFragments.setLayoutParams(params);
        });

        anim.setDuration(1000);
        anim.start();

        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                //ExpRecentPagerAdapter recentPagerAdapter = new ExpRecentPagerAdapter(getSupportFragmentManager());
                //expensePager.setAdapter(recentPagerAdapter);
                if(position != Constants.STAT) pagerRecentExp.setCurrentItem(0);
            }
        });

    }

    // Getter to be Referenced by the containing fragments
    //public LocationViewModel getLocationViewModel() { return locationModel; }
    //public PagerAdapterViewModel getPagerModel() { return pagerModel; }

    public ViewPager2.OnPageChangeCallback getPageChageCallback() {
        return new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                log.i("onPageSelected:%d", position);
                binding.frameTopFragments.removeAllViews();
                //this.position = position;
                //saveMenuItem.setVisible(true);

                switch(position) {
                    case Constants.GAS: // GasManagerFragment
                        pageTitle = getString(R.string.exp_title_gas);
                        binding.frameTopFragments.addView(pagerRecentExp);
                        break;

                    case Constants.SVC:
                        pageTitle = getString(R.string.exp_title_service);
                        binding.frameTopFragments.addView(pagerRecentExp);
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
                super.onPageScrollStateChanged(state);
                log.i("onPageScrollstateChanged: %s", state);
                if(state == 0) {
                    switch(position) {
                        case Constants.GAS:
                            animSlideTopFrame(prevHeight, 150);
                            prevHeight = 150;
                            break;
                        case Constants.SVC:
                            animSlideTopFrame(prevHeight, 120);
                            prevHeight = 120;
                            break;
                        case Constants.STAT:
                            animSlideTopFrame(prevHeight, 300);
                            prevHeight = 300;
                            break;
                    }
                }
            }
        };
    }


}
