package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.CarmanFragmentPagerAdapter;
import com.silverback.carman2.fragments.BoardFragment;
import com.silverback.carman2.fragments.GeneralFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends BaseActivity implements ViewPager.OnPageChangeListener {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    // Objects
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FragmentPagerAdapter pagerAdapter;
    private Fragment[] mFragments;
    private Fragment generalFragment, boardFragment;
    private FragmentTransaction fragmentTransaction;

    private FrameLayout frameLayout;

    // Fields
    private boolean isTabLayoutVisible = false;
    private float toolbarHeight;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        frameLayout = findViewById(R.id.frameLayout);

        // Sets the toolbar used as ActionBar
        setSupportActionBar(toolbar);

        //viewPager = findViewById(R.id.viewPager);
        viewPager = new ViewPager(this);
        viewPager.setId(View.generateViewId());

        // Instantiates FragmentPagerAdapter to have the fragments linked to the viewpager.
        pagerAdapter = new CarmanFragmentPagerAdapter(getSupportFragmentManager());

        // ViewPager and ViewPager.OnPageChageListener attached
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(viewPager);

        // Get Defaults from BaseActivity and pass them to the fragments
        String[] defaults = getDefaultParams();
        Bundle bundle = new Bundle();
        bundle.putStringArray("defaults", defaults);
        log.i("Default Params: %s, %s, %s", defaults[0], defaults[1], defaults[2]);
        // Instantiate FragmentManger and FragmentTransaction to add, replace, or remove a fragment
        generalFragment = new GeneralFragment();
        boardFragment = new BoardFragment();

        // Attach the Fragment of GeneralFragment with the defaults attached.
        generalFragment.setArguments(bundle);
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.frameLayout, generalFragment).commit();

        // Custom method to set TabLayout title and icon, which MUST be invoked after
        // TabLayout.setupWithViewPager as far as TabLayout links with ViewPager.
        addTabIconAndTitle(tabLayout, fragmentTitles, fragmentIcons);

        // Calculate the toolbar height which is a baseline to slide up and down the TabLayout
        // and ViewPager.
        toolbarHeight = getActionbarHeight();
        log.i("toolbar height: %s",  toolbarHeight);

        // Permission Check
        checkPermissions();

        // Custom method to animate the tab layout sliding up and down when clicking the buttons
        // on the toolbar(action bar). The TabLayout moves up and down by changing "Y" property
        // and the ViewPager does so by translating "Y".
        //animSlideTabLayout();
    }

    // Callbacks invoked by Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_board:
                return true;

            case R.id.action_garage:
                animSlideTabLayout();
                return true;

            case R.id.action_login:

                return true;

            case R.id.action_setting:
                startActivity(new Intent(MainActivity.this, GeneralSettingActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Callbacks invoked by ViewPager.OnPageChangeListener
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        log.i("ViewPager Listener");
    }
    @Override
    public void onPageSelected(int position) {
        //Log.i(LOG_TAG, "ViewPager Listeenr");
    }
    @Override
    public void onPageScrollStateChanged(int state) {
        //Log.i(LOG_TAG, "ViewPager Listeenr");
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

        float tabEndValue = (!isTabLayoutVisible)? toolbarHeight : 0;

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frameLayout, "translationY", tabEndValue);
        slideTab.setDuration(500);
        slideViewPager.setDuration(500);

        if(!isTabLayoutVisible) {
            getSupportFragmentManager().beginTransaction().remove(generalFragment).commit();
            frameLayout.addView(viewPager);
        } else {
            frameLayout.removeView(viewPager);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frameLayout, generalFragment).addToBackStack(null).commit();
        }

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
