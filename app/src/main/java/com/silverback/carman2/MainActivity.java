package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.silverback.carman2.fragments.GasFragment;
import com.silverback.carman2.fragments.ServiceFragment;
import com.silverback.carman2.fragments.SettingFragment;
import com.silverback.carman2.fragments.StatFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class MainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    // Constants
    private static final String LOG_TAG = "MainActivity";


    // Objects
    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FragmentPagerAdapter pagerAdapter;
    private Fragment[] mFragments;

    // Fields
    private boolean isTabLayoutVisible = false;
    private float toolbarHeight;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        // Sets the toolbar used as ActionBar
        setSupportActionBar(toolbar);

        // Instantiates FragmentPagerAdapter to have the fragments linked to the viewpager.
        pagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());

        // ViewPager and ViewPager.OnPageChageListener attached
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(viewPager);

        final String[] fragmentTitles = new String[] {
                getString(R.string.tab_gas),
                getString(R.string.tab_service),
                getString(R.string.tab_stat),
                getString(R.string.tab_setting)
        };

        final Drawable[] fragmentIcons = new Drawable[] {
                getDrawable(R.drawable.ic_gas),
                getDrawable(R.drawable.ic_service),
                getDrawable(R.drawable.ic_stats),
                getDrawable(R.drawable.ic_setting)
        };

        toolbarHeight = getActionbarHeight();
        Log.i(LOG_TAG, "toolbar height: "  + toolbarHeight);

        // Custom method to set TabLayout title and icon, which MUST be invoked after
        // TabLayout.setupWithViewPager as far as TabLayout links with ViewPager.
        addTabIconAndTitle(tabLayout, fragmentTitles, fragmentIcons);

        // Custom method to animate the tab layout sliding up and down when clicking the buttons
        // on the toolbar(action bar)
        animSlideTabLayout();


    }

    // Callbacks invoked by Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
        /*
        // Define the listener
        MenuItemCompat.OnActionExpandListener expandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when action item collapses
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Do something when expanded
                return true;  // Return true to expand action view
            }
        };

        // Get the MenuItem for the action item
        //MenuItem actionMenuItem = menu.findItem(R.id.);

        // Assign the listener to that action item
        //MenuItemCompat.setOnActionExpandListener(actionMenuItem, expandListener);

        // Any other things you have to do when creating the options menu...

        return true;
        */
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_board:
                return true;

            case R.id.action_mycar:
                animSlideTabLayout();
                return true;

            case R.id.action_login:

                return true;

            case R.id.action_setting:

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Callbacks invoked by ViewPager.OnPageChangeListener
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        log.i(LOG_TAG, "ViewPager Listener");
    }
    @Override
    public void onPageSelected(int position) {
        //Log.i(LOG_TAG, "ViewPager Listeenr");
    }
    @Override
    public void onPageScrollStateChanged(int state) {
        //Log.i(LOG_TAG, "ViewPager Listeenr");
    }

    private final class CustomFragmentPagerAdapter extends FragmentPagerAdapter {

        CustomFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private final Fragment[] fragments = new Fragment[] {
                new GasFragment(),
                new ServiceFragment(),
                new StatFragment(),
                new SettingFragment()
        };

        @Override
        public int getCount(){
            return fragments.length;
        }

        @Override
        public Fragment getItem(int pos){
            return fragments[pos];
        }

    }

    // Prgramatically, add titles and icons on the TabLayout, which must be invoked after
    // setupWithViewPager when it is linked to ViewPager.
    @SuppressWarnings("ConstantConditions")
    private void addTabIconAndTitle(TabLayout tabLayout, String[] title, Drawable[] icons) {
        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(icons[i]);
            tabLayout.getTabAt(i).setText(title[i]);
        }
    }

    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {
        float tabEndValue = (!isTabLayoutVisible)? 0 : toolbarHeight;

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(viewPager, "translationY", tabEndValue);
        slideTab.setDuration(500);
        slideViewPager.setDuration(500);

        isTabLayoutVisible = !isTabLayoutVisible;

        slideTab.start();
        slideViewPager.start();

    }

    // Measures the size of an android attribute based on ?attr/actionBarSize
    private float getActionbarHeight() {
        TypedValue typedValue = new TypedValue();
        if(getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimension(typedValue.data, getResources().getDisplayMetrics());
        }
        return -1;
    }

}
