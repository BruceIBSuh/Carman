package com.silverback.carman;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
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

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import com.silverback.carman.adapters.ExpContentPagerAdapter;
import com.silverback.carman.adapters.ExpRecentAdapter;
import com.silverback.carman.databinding.ActivityExpenseBinding;
import com.silverback.carman.fragments.GasManagerFragment;
import com.silverback.carman.fragments.ServiceManagerFragment;
import com.silverback.carman.fragments.StatGraphFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
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
    private final int MENU_ITEM_SAVE = 1000;
    private final int GAS = 0;
    private final int SVC = 1;
    private final int STAT = 2;

    // Objects
    private ActivityExpenseBinding binding;
    private LocationViewModel locationModel;
    private PagerAdapterViewModel pagerModel;
    private StationListViewModel stnListModel;
    private ExpContentPagerAdapter expContentPagerAdapter;
    //private GasManagerFragment gasManager;
    //private ServiceManagerFragment svcManager;
    private StatGraphFragment statFragment;

    private ThreadTask tabPagerTask;
    private ThreadTask locationTask;
    private ThreadTask stationListTask;

    private ViewPager2 pagerRecentExp;
    private MenuItem saveMenuItem;
    private ViewTreeObserver vto;

    // Fields
    private int currentPage;
    private int prevHeight;
    private int category;
    private float tabHeight;
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

        // Create objects
        //gasManager = (GasManagerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_gas);
        //svcManager = (ServiceManagerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_svc);
        //statFragment = (StatGraphFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_stat);


        // Define ViewModels. ViewModelProviders.of(this) is deprecated.
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        pagerModel = new ViewModelProvider(this).get(PagerAdapterViewModel.class);
        stnListModel = new ViewModelProvider(this).get(StationListViewModel.class);

        // Set the toolbar as the working action bar
        setSupportActionBar(binding.toolbarExpense);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        binding.appBar.addOnOffsetChangedListener(this);
        pageTitle = getString(R.string.exp_title_gas); //default title when the appbar scrolls up.

        // Add the content fragment(gas/service/stat) to the ViewPager

        expContentPagerAdapter = new ExpContentPagerAdapter(getSupportFragmentManager(), getLifecycle());
        binding.pagerTabFragment.setAdapter(expContentPagerAdapter);
        //binding.pagerTabFragment.setCurrentItem(0);
        binding.pagerTabFragment.registerOnPageChangeCallback(onPageChangeCallback());

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


        String jsonSvcItems = mSettings.getString(Constants.SERVICE_ITEMS, null);
        //String jsonDistrict = mSettings.getString(Constants.DISTRICT, null);
        tabPagerTask = sThreadManager.startExpenseTabPagerTask(pagerModel, jsonSvcItems);

        pagerRecentExp = new ViewPager2(this);
        pagerRecentExp.setId(View.generateViewId());
        ExpRecentAdapter recentAdapter = new ExpRecentAdapter(getSupportFragmentManager(), getLifecycle());
        pagerRecentExp.setAdapter(recentAdapter);
        pagerRecentExp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
            @Override
            public void onPageSelected(int position) {
                log.i("onPageSelected");
            }
        });

        // Consider this process should be behind the layout to lessen the ram load.
        /*
        if(!isGeofencing) {
            locationTask = sThreadManager.fetchLocationTask(this, locationModel);
            locationModel.getLocation().observe(this, location -> {
                if(mPrevLocation == null || location.distanceTo(mPrevLocation) > Constants.UPDATE_DISTANCE) {
                    final String[] defaults = getDefaultParams();
                    defaults[1] = Constants.MIN_RADIUS;
                    stationListTask = sThreadManager.startStationListTask(stnListModel, location, defaults);
                    mPrevLocation = location;
                }
            });
        }
        */

        // Get the tab hegiht after the view has drawn.
        /*
        binding.frameLayoutExpense.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        binding.frameLayoutExpense.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
        */

    }

    @Override
    public void onResume() {
        super.onResume();
        log.i("onResume");
    }


    @Override
    public void onPause() {
        super.onPause();
        log.i("onPause");

    }

    @Override
    public void onStop(){
        super.onStop();
        log.i("onStop");
        //if(locationTask != null) locationTask = null;
        //if(tabPagerTask != null) tabPagerTask = null;
        //if(stationListTask != null) stationListTask = null;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        //Menu.add(groupId, itemId, order, title)
        MenuItem save = menu.add(Menu.NONE, MENU_ITEM_SAVE, Menu.NONE, R.string.exp_menu_save);
        //saveMenuItem = menu.findItem(MENU_ITEM_ID);
        save.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        save.setIcon(R.drawable.ic_save_room);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                if (isGeofencing) {
                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.putExtra("isGeofencing", true);
                    startActivity(mainIntent);
                } else finish();

                break;

            // menu for saving the gas or service data
            case MENU_ITEM_SAVE:
                saveExpenseData(currentPage);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // AppBarLayout.OnOffsetChangeListener invokes this method
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int scroll) {
        binding.appBar.post(() -> {
            if(Math.abs(scroll) == 0) getSupportActionBar().setTitle(getString(R.string.exp_toolbar_title));
            else if(Math.abs(scroll) == binding.appBar.getTotalScrollRange())
                getSupportActionBar().setTitle(pageTitle);
        });

        // Fade the topFrame accroding to the scrolling of the AppBarLayout
        //setBackgroundOpacity(appBar.getTotalScrollRange(), scroll); //fade the app

        //float bgAlpha = (float)((100 + (scroll * 100 / binding.appBar.getTotalScrollRange())) * 0.01);
        //binding.frameLayoutExpense.setAlpha(bgAlpha);

    }

    // Animate TabLayout and the tap-syned viewpager sequentially. As the animation completes,
    // the top viewpager is set up with ExpRecntPagerAdapter and add the viewpager to the frame and
    // start LocationTask.
    private void animSlideTabLayout() {
        final float toolbarHeight = getActionbarHeight();
        log.i("tab height: %s", tabHeight);
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator tab = ObjectAnimator.ofFloat(binding.tabExpense, "translationY", toolbarHeight);
        ObjectAnimator frame = ObjectAnimator.ofFloat(binding.frameLayoutExpense, "translationY", toolbarHeight);
        tab.setDuration(1000);
        frame.setDuration(1000);
        animSet.play(tab).before(frame);

        animSet.addListener(new AnimatorListenerAdapter(){
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                //if(binding.frameLayoutExpense.getChildCount() > 0) binding.frameLayoutExpense.removeAllViews();
                //animSlideTopFrame(0, 150);
                //prevHeight = 100;
                //binding.frameLayoutExpense.addView(pagerRecentExp);

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
        ViewGroup.LayoutParams params = binding.frameLayoutExpense.getLayoutParams();

        anim.addUpdateListener(valueAnimator -> {
            params.height = (Integer)valueAnimator.getAnimatedValue();;
            binding.frameLayoutExpense.setLayoutParams(params);
        });

        anim.setDuration(500);
        anim.start();

        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);

                //ExpRecentAdapter recentAdapter = new ExpRecentAdapter(getSupportFragmentManager(), getLifecycle());
                //pagerRecentExp.setAdapter(recentAdapter);
                //if(position != Constants.STAT) pagerRecentExp.setCurrentItem(0);
            }
        });

    }


    private ViewPager2.OnPageChangeCallback onPageChangeCallback() {
        return  new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                log.i("current page: %d", currentPage);
                if(binding.frameLayoutExpense.getChildCount() > 0)
                    binding.frameLayoutExpense.removeAllViews();
                //saveMenuItem.setVisible(true);
                switch (position) {
                    case GAS: // GasManagerFragment
                        pageTitle = getString(R.string.exp_title_gas);
                        //saveMenuItem.setVisible(true);
                        binding.frameLayoutExpense.addView(pagerRecentExp);
                        break;

                    case SVC:
                        pageTitle = getString(R.string.exp_title_service);
                        //saveMenuItem.setVisible(true);
                        binding.frameLayoutExpense.addView(pagerRecentExp);
                        break;

                    case STAT:
                        pageTitle = getString(R.string.exp_title_stat);
                        //saveMenuItem.setVisible(false);
                        StatGraphFragment statGraphFragment = new StatGraphFragment();
                        //if(statFragment == null) statFragment = new StatGraphFragment();
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frameLayout_expense, statGraphFragment).commit();

                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if(state == 0) {
                    switch(currentPage) {
                        case GAS:
                            animSlideTopFrame(prevHeight, 120);
                            prevHeight = 120;
                            break;
                        case SVC:
                            animSlideTopFrame(prevHeight, 100);
                            prevHeight = 100;
                            break;
                        case STAT:
                            animSlideTopFrame(prevHeight, 200);
                            prevHeight = 200;
                            break;
                    }

                }
            }
        };

    }

    private void saveExpenseData(int page) {
        Fragment fragment = expContentPagerAdapter.createFragment(page);
        boolean isSaved = false;
        switch(page) {
            case GAS:
                isSaved = ((GasManagerFragment)fragment).saveGasData();
                break;
            case SVC:
                isSaved = ((ServiceManagerFragment)fragment).saveServiceData();
                break;
        }

        if(isSaved) finish();
    }
}
