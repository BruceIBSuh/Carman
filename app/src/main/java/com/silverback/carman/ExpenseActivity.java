package com.silverback.carman;

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

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
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
 * StatStmtsFragment. The other is at the top to hold MainPricePagerFragment w/ MainPricePagerAdapter which,
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
    //private LocationViewModel locationModel;
    //private PagerAdapterViewModel pagerModel;
    private StationListViewModel stnListModel;
    private ExpContentPagerAdapter expContentPagerAdapter;
    //private GasManagerFragment gasManager;
    //private ServiceManagerFragment svcManager;
    private StatGraphFragment statGraphFragment;

    private ThreadTask tabPagerTask;
    private ThreadTask locationTask;
    private ThreadTask stationListTask;

    private ViewPager2 pagerRecentExp;
    private MenuItem menuSave;
    private ViewTreeObserver vto;

    // Fields
    private int currentPage;
    private int prevHeight = 0;
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

        // Check if the activity has got started by tabbing the geofence notification.
        if(getIntent().getAction() != null) {
            if(getIntent().getAction().equals(Constants.NOTI_GEOFENCE)) {
                isGeofencing = true;
                category = getIntent().getIntExtra(Constants.GEO_CATEGORY, -1);
            }
        }

        String userId = getUserIdFromStorage(this);
        // Create objects
        //gasManager = (GasManagerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_gas);
        //svcManager = (ServiceManagerFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_svc);
        //statFragment = (StatGraphFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_stat);


        // Define ViewModels. ViewModelProviders.of(this) is deprecated.
        LocationViewModel locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        PagerAdapterViewModel pagerModel = new ViewModelProvider(this).get(PagerAdapterViewModel.class);
        stnListModel = new ViewModelProvider(this).get(StationListViewModel.class);

        // Set the toolbar as the working action bar
        setSupportActionBar(binding.toolbarExpense);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        pageTitle = getString(R.string.exp_title_gas); //default title when the appbar scrolls up.
        binding.appBar.addOnOffsetChangedListener(this);

        // Add the content fragment(gas/service/stat) to the ViewPager
        expContentPagerAdapter = new ExpContentPagerAdapter(this);
        binding.pagerTabFragment.setAdapter(expContentPagerAdapter);
        binding.pagerTabFragment.registerOnPageChangeCallback(addPageChangeCallback());

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
                log.i("onPageSelected: %s", position);
            }
        });

        // Consider this process should be behind the layout to lessen the ram load.
        if(!isGeofencing) {
            locationTask = sThreadManager.fetchLocationTask(this, locationModel);
            locationModel.getLocation().observe(this, location -> {
                if(location == null) return;
                if (mPrevLocation == null || location.distanceTo(mPrevLocation) > Constants.UPDATE_DISTANCE) {
                    mPrevLocation = location;
                    String[] defaults = getNearStationParams();
                    defaults[1] = Constants.MIN_RADIUS;
                    stationListTask = sThreadManager.startStationListTask(stnListModel, location, defaults);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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
        menuSave = menu.add(Menu.NONE, MENU_ITEM_SAVE, Menu.NONE, R.string.exp_menu_save);
        menuSave.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuSave.setIcon(R.drawable.ic_save_room);

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

                }

                setResult(RESULT_CANCELED); //to make StationRecyclerView gone if it is visible
                finish();
                return true;

            // menu for saving the gas or service data
            case MENU_ITEM_SAVE:
                return saveExpenseData(currentPage);

            default: return super.onOptionsItemSelected(item);
        }
    }

    // Modify the options menu based on events that occur during the activity lifecycle.
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuSave = menu.findItem(MENU_ITEM_SAVE);
        menuSave.setVisible(currentPage != STAT);
        return true;
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
        float bgAlpha = (float)((100 + (scroll * 100 / binding.appBar.getTotalScrollRange())) * 0.01);
        binding.frameExpense.setAlpha(bgAlpha);

    }

    // Animate TabLayout and the tap-syned viewpager sequentially. As the animation completes,
    // the top viewpager is set up with ExpRecntPagerAdapter and add the viewpager to the frame and
    // start LocationTask.
    private void animSlideTabLayout() {
        final float toolbarHeight = getActionbarHeight();
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator animTab = ObjectAnimator.ofFloat(binding.tabExpense, "translationY", toolbarHeight);
        ObjectAnimator animFrame = ObjectAnimator.ofFloat(binding.frameExpense, "translationY", toolbarHeight);
        animTab.setDuration(1000);
        animFrame.setDuration(1000);
        animSet.play(animTab).before(animFrame);
        animSet.start();
        /*
        animSet.addListener(new AnimatorListenerAdapter(){
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                log.i("Animate tab and frame");
                // In case that this activity is started by the geofence notification, ServiceFragment
                // must be set to the current page only after the viewpager at the top has added to
                // the framelayout. Otherwise, an error occurs due to no child view in the viewpager.
                if(isGeofencing && category == Constants.SVC)
                    binding.pagerTabFragment.setCurrentItem(category);
            }
        });

         */

    }

    private void animSlideTopFrame(int oldY, int newY) {
        // Convert the dp unit to pixels
        int prevHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                oldY, getResources().getDisplayMetrics());
        int newHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newY, getResources().getDisplayMetrics());

        // Animate to slide the top frame down to the measured height.
        ValueAnimator anim = ValueAnimator.ofInt(prevHeight, newHeight);
        ViewGroup.LayoutParams params = binding.frameExpense.getLayoutParams();
        anim.setDuration(1000);
        anim.start();

        anim.addUpdateListener(valueAnimator -> {
            params.height = (Integer)valueAnimator.getAnimatedValue();;
            binding.frameExpense.setLayoutParams(params);
        });
    }

    // ViewPager2 contains OnPageChangeCallback as an abstract class. OnPageChangeCallback of
    // ViewPager was interface which is required to implement onPageSelected, onPageScrollStateChanged,
    // and onPageScrolled.
    private ViewPager2.OnPageChangeCallback addPageChangeCallback() {
        return  new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                if(binding.frameExpense.getChildCount() > 0) binding.frameExpense.removeAllViews();
                // Invoke onPrepareOptionsMenu(Menu)
                invalidateOptionsMenu();

                switch (position) {
                    case GAS: // GasManagerFragment
                        pageTitle = getString(R.string.exp_title_gas);
                        binding.frameExpense.addView(pagerRecentExp);
                        pagerRecentExp.setCurrentItem(0);
                        animSlideTopFrame(prevHeight, 120);
                        prevHeight = 120;
                        break;

                    case SVC:
                        pageTitle = getString(R.string.exp_title_service);
                        binding.frameExpense.addView(pagerRecentExp);
                        pagerRecentExp.setCurrentItem(0);
                        animSlideTopFrame(prevHeight, 100);
                        prevHeight = 100;
                        break;

                    case STAT:
                        pageTitle = getString(R.string.exp_title_stat);
                        menuSave.setVisible(false);
                        StatGraphFragment statGraphFragment = new StatGraphFragment();
                        //if(statGraphFragment == null) statGraphFragment = new StatGraphFragment();
                        //which seems not working.
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frame_expense, statGraphFragment).commit();

                        animSlideTopFrame(prevHeight, 200);
                        prevHeight = 200;
                        break;
                }
            }

        };

    }

    private boolean saveExpenseData(int page) {
        Fragment fragment = expContentPagerAdapter.createFragment(page);
        String userId = getUserIdFromStorage(this);
        boolean isSaved = false;
        switch(page) {
            case GAS:
                isSaved = ((GasManagerFragment)fragment).saveGasData(userId);
                break;
            case SVC:
                isSaved = ((ServiceManagerFragment)fragment).saveServiceData();
                break;
        }

        if(isSaved) {
            setResult(RESULT_CANCELED);
            finish();
        }

        return isSaved;

    }
}
