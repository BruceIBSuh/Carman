package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.CarmanFragmentPagerAdapter;
import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.RecentExpFragment;
import com.silverback.carman2.fragments.StatGraphFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.utils.CustomPagerIndicator;
import com.silverback.carman2.views.StatGraphView;

import java.util.List;

public class ExpenseActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseActivity.class);

    // Constants
    private static final int TAB_CARMAN = 1;
    private static final int TAB_BOARD = 2;
    private static final int NumOfPages = 5;

    // Objects
    private GasManagerFragment gasFragment;
    private RecentExpFragment expFragment;
    private StatGraphFragment graphFragment;
    private AppBarLayout appBar;
    private TabLayout tabLayout;
    private FrameLayout frameTop;
    private List<String> tabTitleList;
    private List<Drawable> tabIconList;
    private ViewPager tabPager;
    private StatGraphFragment statGraphFragment;
    private StatGraphView statGraphView;
    private CustomPagerIndicator indicator;


    // Fields
    private boolean isTabVisible = false;
    private String pageTitle;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        // UIs
        appBar = findViewById(R.id.appBar);
        appBar.addOnOffsetChangedListener(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);

        frameTop = findViewById(R.id.frame_top);
        FrameLayout frameFragments = findViewById(R.id.frame_fragments);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.exp_toolbar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //TEST CODING
        pageTitle = "GasManager";

        // ViewPager and Indicator
        //tabViewPager = findViewById(R.id.viewPager_tap);
        tabPager = new ViewPager(this);
        tabPager.setId(View.generateViewId());
        frameFragments.addView(tabPager);

        // TEMPORARY CODING for ServiceList items which should be saved in SharedPreferences
        // as a Json-fomatted string.
        FragmentPagerAdapter pagerAdapter =
                new CarmanFragmentPagerAdapter(this, getSupportFragmentManager());

        tabPager.setAdapter(pagerAdapter);
        tabPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(tabPager);

        addTabIconAndTitle(this, tabLayout);
        animSlideTabLayout();

        // ViewPager to display receent 5 expenses on top of the screen.
        expFragment = new RecentExpFragment();
        graphFragment = new StatGraphFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.frame_top, expFragment, "expFragment")
                .addToBackStack(null)
                .commit();
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

        menu.add(Menu.NONE, 1000, Menu.NONE, "SAVE");
        MenuItem item = menu.findItem(1000);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(R.drawable.ic_toolbar_save);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsItemSelected in GeneralSettingActivity");
            finish();
            return true;
        } else {
            log.i("SAVE button clicked");
        }

        return super.onOptionsItemSelected(item);
    }

    // The following 3 overriding methods are invoked by ViewPager.OnPageChangeListener.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        log.i("onPageScrolled: %s", position);
    }
    @Override
    public void onPageSelected(int position) {

        // When displaying the recent expense viewpager, FrameLayout(frameTop) holds the custom
        // view which consists of ViewPager and Indicator.
        switch(position) {
            case 0:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_top, expFragment)
                        .commit();
                // TEST CODING
                pageTitle = "GasManager";
                break;
            case 1:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_top, expFragment)
                        .commit();
                // TEST CODING
                pageTitle = "ServiceManager";
                break;
            case 2:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_top, graphFragment)
                        .commit();
                // TEST CODING
                pageTitle = "Statistics";
                break;
        }
        /*

        if(position == 0 || position == 1) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_top, expFragment)
                    .commit();

        } else if(position == 2) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_top, graphFragment)
                    .commit();
        }
        */
    }
    @Override
    public void onPageScrollStateChanged(int state) {
        log.i("onPageScrollStateChanged");
    }

    // Invoked by AppBarLayout.OnOffsetChangeListener to be informed whether the scroll is located
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        log.i("AppBar scrolling state: %s", i);
        log.i("AppBar Total Scroll Range: %s", appBar.getTotalScrollRange());
        if(Math.abs(i) == appBar.getTotalScrollRange())
            getSupportActionBar().setTitle(pageTitle);
    }

    /*
    // Prgramatically, add titles and icons on the TabLayout, which must be invoked after
    // setupWithViewPager when it is linked to ViewPager.
    @SuppressWarnings("ConstantConditions")
    private int addTabIconAndTitle(int tab) {

        //if(!tabTitleList.isEmpty()) tabTitleList.clear();
        //if(!tabIconList.isEmpty()) tabIconList.clear();
        switch(tab) {
            case TAB_CARMAN:
                tabTitleList = Arrays.asList(getResources().getStringArray(R.array.tab_carman_title));
                Drawable[] icons = {
                        getDrawable(R.drawable.ic_gas),
                        getDrawable(R.drawable.ic_service),
                        getDrawable(R.drawable.ic_stats)};

                tabIconList = Arrays.asList(icons);
                break;

            case TAB_BOARD:
                tabTitleList = Arrays.asList(getResources().getStringArray(R.array.tap_board_title));
                icons = new Drawable[]{};
                tabIconList = Arrays.asList(icons);
                break;

        }

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            log.i("Title: %s", tabTitleList.get(i));
            tabLayout.getTabAt(i).setText(tabTitleList.get(i));
            if(!tabIconList.isEmpty()) tabLayout.getTabAt(i).setIcon(tabIconList.get(i));
        }

        return tab;
    }
    */



    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {
        float toolbarHeight = getActionbarHeight();
        float tabEndValue = (!isTabVisible)? toolbarHeight : 0;

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frameTop, "translationY", tabEndValue);
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
    */


}
