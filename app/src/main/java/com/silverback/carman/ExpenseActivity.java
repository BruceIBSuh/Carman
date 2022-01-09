package com.silverback.carman;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.silverback.carman.adapters.ExpensePagerAdapter;
import com.silverback.carman.adapters.ExpRecentAdapter;
import com.silverback.carman.databinding.ActivityExpenseBinding;
import com.silverback.carman.fragments.GasManagerFragment;
import com.silverback.carman.fragments.MemoPadFragment;
import com.silverback.carman.fragments.NumberPadFragment;
import com.silverback.carman.fragments.ServiceManagerFragment;
import com.silverback.carman.fragments.StatGraphFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.StationListTask;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.DatePickerFragment;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.lang.ref.WeakReference;
import java.util.Objects;

/*
 * This activity is largely compolsed of two viewpagers. One viewpager which is synced w/ the tab
 * is at the bottom for holding such fragments as GasManagerFragment, ServiceManagerFragment, and
 * StatStmtsFragment. The other is at the top to hold MainPricePagerFragment w/ MainPricePagerAdapter
 * which, as a single fragment with multiple pages, shows the recent 5 expense statements of the first
 * 2 tab-synced fragments and a single page of StatGraphFragment of the last tab-synced one, which
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
    private final int GAS = 0;
    private final int SVC = 1;
    private final int STAT = 2;

    // Objects
    private ActivityExpenseBinding binding;
    private LocationViewModel locationModel;
    private ExpensePagerAdapter pagerAdapter;
    private StatGraphFragment statGraphFragment;
    private NumberPadFragment numPad;
    private MemoPadFragment memoPad;

    //private ThreadTask tabPagerTask;
    private LocationTask locationTask;
    private StationListTask stationListTask;
    private Location mPrevLocation;

    private ViewPager2 recentExpensePager;
    private MenuItem menuSave;

    // Fields
    private int currentPage;
    private int category;
    private String pageTitle;
    private boolean isGeofencing;
    private int prevHeight;
    //private int totalExpense;
    private boolean isCollapsed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if the activity gets started by tabbing the geofence notification.
        final String action = getIntent().getAction();
        if(TextUtils.isEmpty(action) && Objects.equals(action, Constants.NOTI_GEOFENCE)){
            isGeofencing = true;
            category = getIntent().getIntExtra(Constants.GEO_CATEGORY, -1);
        }

        statGraphFragment = new StatGraphFragment();
        numPad = new NumberPadFragment();
        memoPad = new MemoPadFragment();

        // Create initial layouts of the appbar, tablayout, and viewpager on the top.
        createAppbarLayout();
        createTabLayout();
        createExpenseViewPager();
        binding.appbar.addOnOffsetChangedListener(this);

        // ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        FragmentSharedModel fragmentModel = new ViewModelProvider(this).get(FragmentSharedModel.class);

        // Upon saving and uploading the expense data, back to MainActivity w/ the activity result
        fragmentModel.getTotalExpenseByCategory().observe(this, totalExpense -> {
            if(totalExpense > 0) {
                binding.pagerTabFragment.unregisterOnPageChangeCallback(addPageChangeCallback());
                // Back to MainActivity w/ results
                Intent resultIntent = new Intent();
                resultIntent.putExtra("expense", totalExpense);
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            }
        });

        // Init the task to get the current location.
        locationTask = sThreadManager.fetchLocationTask(this, locationModel);
        // Worker Thread for getting service items and the current gas station.
        //String jsonSvcItems = mSettings.getString(Constants.SERVICE_ITEMS, null);
        //tabPagerTask = sThreadManager.startExpenseTabPagerTask(pagerModel, jsonSvcItems);
        // Initialize field values
        prevHeight = 0;

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
        binding.pagerTabFragment.unregisterOnPageChangeCallback(addPageChangeCallback());
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;

    }

    // Clear focus out of any EditText, no matter it is in child fragments. MotionEvent.ACTION_
    // BUTTON_PRESS should be excluded for scrolling.
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN ){//&& event.getAction() != MotionEvent.ACTION_BUTTON_PRESS) {
            View v= getCurrentFocus();
            if(v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if(!outRect.contains((int)event.getRawX(), (int)event.getRawY())){
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.options_expense, menu);
        menuSave = menu.findItem(R.menu.options_expense);
        return true;
        //return super.onCreateOptionsMenu(menu);
    }
    // Modify the options menu based on events that occur during the activity lifecycle.
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuSave = menu.findItem(R.id.save_expense);
        menuSave.setVisible(currentPage != STAT);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            // DEBUG & REFACTOR REQURIED
            if(isGeofencing) {
                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.putExtra("isGeofencing", true);
                startActivity(mainIntent);
            } else finish();
            return true;

        } else if(item.getItemId() == R.id.save_expense) {
            saveExpenseData();
            return true;

        } else return super.onOptionsItemSelected(item);
    }

    // Implement AppBarLayout.OnOffsetChangeListener
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int scroll) {
        binding.appbar.post(() -> {
            if(Math.abs(scroll) == 0) {
                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.exp_toolbar_title));
                isCollapsed = false;
            } else if(Math.abs(scroll) == binding.appbar.getTotalScrollRange()) {
                Objects.requireNonNull(getSupportActionBar()).setTitle(pageTitle);
                isCollapsed = true;
            }
        });

        // Fade the topFrame accroding to the scrolling of the AppBarLayout
        //setBackgroundOpacity(appBar.getTotalScrollRange(), scroll); //fade the app
        if(binding.appbar.getTotalScrollRange() != 0) {
            float bgAlpha = (float)((100 + (scroll * 100 / binding.appbar.getTotalScrollRange())) * 0.01);
            binding.topFrame.setAlpha(bgAlpha);
        }
    }

    // ViewPager2 contains OnPageChangeCallback as an abstract class. OnPageChangeCallback of
    // ViewPager was interface which is required to implement onPageSelected, onPageScrollStateChanged,
    // and onPageScrolled.
    private ViewPager2.OnPageChangeCallback addPageChangeCallback() {
        return  new ViewPager2.OnPageChangeCallback() {
            /*
            int state;// 0 -> idle, 1 -> dragging 2 -> settling
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                this.state = state;
            }
             */

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                //if(state > 0 && position != STAT) return;
                // To prevent the ServiceManagerFragment from being called twice. Not sure why it
                // is called twice. Seems a bug in ViewPager2.
                //if(state > 0) return;
                currentPage = position;
                if(binding.topframeExpense.getChildCount() > 0){
                    binding.topframeExpense.removeAllViews();
                }
                // Invoke onPrepareOptionsMenu(Menu)
                invalidateOptionsMenu();
                binding.topframePage.setVisibility(View.VISIBLE);



                switch (position) {
                    case GAS:
                        pageTitle = getString(R.string.exp_title_gas);
                        recentExpensePager.setCurrentItem(0);
                        binding.topframeExpense.addView(recentExpensePager);
                        animSlideTopFrame(prevHeight, 120);
                        prevHeight = 120;
                        break;

                    case SVC:
                        pageTitle = getString(R.string.exp_title_service);
                        recentExpensePager.setCurrentItem(0);
                        binding.topframeExpense.addView(recentExpensePager);
                        animSlideTopFrame(prevHeight, 100);
                        prevHeight = 100;
                        break;

                    case STAT:
                        pageTitle = getString(R.string.exp_title_stat);
                        menuSave.setVisible(false);
                        binding.topframePage.setVisibility(View.GONE);
                        statGraphFragment = new StatGraphFragment();
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.topframe_expense, statGraphFragment)
                                .commit();
                        animSlideTopFrame(prevHeight, 190);
                        prevHeight = 190;
                        break;
                }
                //if(isCollapsed) animExpandTabLayout();
                if(isCollapsed) binding.appbar.setExpanded(true);
            }
        };
    }

    // Create the appbarlayout
    private void createAppbarLayout() {
        setSupportActionBar(binding.toolbarExpense);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        pageTitle = getString(R.string.exp_title_gas); //default title when the appbar scrolls up.

    }

    private void createTabLayout() {
        pagerAdapter = new ExpensePagerAdapter(getSupportFragmentManager(), getLifecycle());
        binding.pagerTabFragment.setAdapter(pagerAdapter);
        binding.pagerTabFragment.registerOnPageChangeCallback(addPageChangeCallback());

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
        }).attach();

        animSlideTabLayout();
    }

    private void createExpenseViewPager() {
        // Add the viewpager in the framelayout.
        recentExpensePager = new ViewPager2(this);
        ExpRecentAdapter recentAdapter = new ExpRecentAdapter(getSupportFragmentManager(), getLifecycle());
        recentExpensePager.setAdapter(recentAdapter);
        recentExpensePager.setCurrentItem(0);
        new TabLayoutMediator(binding.topframePage, recentExpensePager, true, true,
                (tab, pos) -> {}).attach();
    }

    private void animExpandTabLayout() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)binding.appbar.getLayoutParams();
        final AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        if (behavior != null) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt();
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.addUpdateListener(animation -> {
                behavior.setTopAndBottomOffset((int)animation.getAnimatedValue());
                binding.appbar.requestLayout();
            });
            valueAnimator.setIntValues(0, 0);
            valueAnimator.setDuration(5000);
            valueAnimator.start();
        }
    }
    // Animate TabLayout and the tap-syned viewpager sequentially. As the animation completes,
    // the top viewpager is set up with ExpRecntPagerAdapter and add the viewpager to the frame and
    // start LocationTask.
    private void animSlideTabLayout() {
        float tbHeight = getActionbarHeight();
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator animTab = ObjectAnimator.ofFloat(binding.tabExpense, "translationY", tbHeight);
        ObjectAnimator animFrame = ObjectAnimator.ofFloat(binding.topFrame, "translationY", tbHeight);
        animTab.setDuration(1000);
        animFrame.setDuration(1000);
        animSet.play(animTab).before(animFrame);
        animSet.start();

        // In case the app gets inititated by Geofence notification, ServiceFragent must be set to
        // the current page only after the topframe viewpager has added. Otherwise, an error occurs
        // for the reason of no child view in the viewpager.
        /*
        animSet.addListener(new AnimatorListenerAdapter(){
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                if(isGeofencing && category == Constants.SVC)
                    binding.pagerTabFragment.setCurrentItem(category);

                // Fetch the current loaction and visiting gas station based on it only after this
                // animation ends to lessen the main thread load.
                fetchCurrentStation(ExpenseActivity.this);
            }
        });

         */
    }

    // Scale down or up the height of the viewpager container based on the content length in the
    // viewpager
    private void animSlideTopFrame(int oldY, int newY) {
        // Convert the dp unit to pixels
        ViewGroup.LayoutParams params = binding.topframeExpense.getLayoutParams();
        int prevHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                oldY, getResources().getDisplayMetrics());
        int newHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newY, getResources().getDisplayMetrics());


        // Animate to slide the top frame down to the measured height.
        ValueAnimator anim = ValueAnimator.ofInt(prevHeight, newHeight);
        anim.setDuration(1000);
        anim.start();

        anim.addUpdateListener(valueAnimator -> {
            params.height = (int)valueAnimator.getAnimatedValue();
            binding.topframeExpense.setLayoutParams(params);
        });
    }

    // Once a current location is fetched, get the current gas station based on it. This method
    // will be referenced in ServiceManagerFragment in the ViewCreate lifecycle.
    private void fetchCurrentStation(LifecycleOwner lifeCycleOwner) {
        locationModel.getLocation().observe(lifeCycleOwner, location -> {
            if(location == null) return;
            if (mPrevLocation == null || location.distanceTo(mPrevLocation) > Constants.UPDATE_DISTANCE) {
                mPrevLocation = location;
                String[] defaults = getNearStationParams();
                defaults[1] = Constants.MIN_RADIUS;
                StationListViewModel stnListModel = new ViewModelProvider(this).get(StationListViewModel.class);
                stationListTask = sThreadManager.startStationListTask(stnListModel, location, defaults);
            }
        });
    }

    // Implement change time button onClickListener
    public void setCustomTime(View view) {
        DialogFragment datePickerFragment = new DatePickerFragment();
        datePickerFragment.show(getSupportFragmentManager(), "datePicker");
    }

    // Pop up the number pad which is invoked in GasManagerFragment and ServiceManagerFragment
    // in common and defined in each xml layou files as onClick.
    public void showNumPad(View view) {
        // return true if the fragment is currently added to its activity, preventing the click event
        // from repeating.
        if(numPad.isAdded()) return;
        Bundle args = new Bundle();
        String value = ((TextView)view).getText().toString();
        args.putInt("viewId", view.getId());
        args.putString("initValue", value);
        numPad.setArguments(args);
        numPad.show(getSupportFragmentManager(), "numberPad");

    }

    public void showServiceItemMemo(View view) {
        if(memoPad.isAdded()) return;
        Bundle args = new Bundle();
        String value = ((TextView)view).getText().toString();
        args.putInt("viewId", view.getId());
        args.putString("memo", value);
        memoPad.setArguments(args);
        memoPad.show(getSupportFragmentManager(), "memoPad");
    }

    // Save the form data in the Room based on which framgnet the activity contains. The data should
    // be uploaded to Firestore at the same time only if the user is logged in. The method to save
    // data is defined in each fragment.
    private void saveExpenseData() {
        //WeakReference<Fragment> weakFragment = pagerAdapter.weakFragmentReference(currentPage);
        Fragment fragment = pagerAdapter.getCurrentFragment(currentPage);
        String userId = getUserIdFromStorage(this);
        if(fragment instanceof GasManagerFragment) {
            ((GasManagerFragment)fragment).saveGasData(userId);
        } else if(fragment instanceof ServiceManagerFragment) {
            ((ServiceManagerFragment)fragment).saveServiceData(userId);
        }
    }

    //Invoked when the favorite image button clicks, which is defined in android:onClick in the xml
    public void checkBackgroundLocationPermission(View view) {
        WeakReference<Fragment> weakFragment = pagerAdapter.weakFragmentReference(currentPage);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
            checkRuntimePermission(binding.getRoot(), permission, () -> {
                if(weakFragment.get() instanceof GasManagerFragment) {
                    ((GasManagerFragment)weakFragment.get()).addGasFavorite();
                } else if(weakFragment.get() instanceof ServiceManagerFragment) {
                    ((ServiceManagerFragment)weakFragment.get()).addServiceFavorite();
                }
            });
        } else {
            if(weakFragment.get() instanceof GasManagerFragment) {
                ((GasManagerFragment)weakFragment.get()).addGasFavorite();
            } else if(weakFragment.get() instanceof ServiceManagerFragment) {
                ((ServiceManagerFragment)weakFragment.get()).addServiceFavorite();
            }
        }
    }

}
