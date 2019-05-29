package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BillboardPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class BillboardActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener, AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BillboardActivity.class);

    // Objects
    private AppBarLayout appBar;
    private TabLayout tabLayout;
    private ViewPager boardPager;

    // Fields
    private boolean isTabVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        //FrameLayout framePager = findViewById(R.id.frame_pager_board);
        ViewPager boardPager = findViewById(R.id.viewpager_board);
        appBar = findViewById(R.id.appBar);
        tabLayout = findViewById(R.id.tabLayout);

        // Add an listener to AppBarLayout
        appBar.addOnOffsetChangedListener(this);

        // Set Toolbar and its title as AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Hello Billboard");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //boardPager = new ViewPager(this);
        //boardPager.setId(View.generateViewId());
        //framePager.addView(boardPager);

        BillboardPagerAdapter pagerAdapter = new BillboardPagerAdapter(getSupportFragmentManager());
        boardPager.setAdapter(pagerAdapter);
        boardPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(boardPager);


        addTabIconAndTitle(this, tabLayout);
        animSlideTabLayout();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsItemSelected in SettingActivity");
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // AppBarLayout.OnOffsetChangedListener invokes this.
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {

    }

    // ViewPager.OnPageChangeListener invokes the following 3 overriding methods.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }
    @Override
    public void onPageSelected(int position) {
        switch(position) {
            case 0:
                break;

            case 1:
                break;

            case 2:
                break;
        }
    }
    @Override
    public void onPageScrollStateChanged(int state) {

    }

    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {

        float toolbarHeight = getActionbarHeight();
        float tabEndValue = (!isTabVisible)? toolbarHeight : 0;
        log.i("anim:%s", tabEndValue);

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        //ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frameTop, "translationY", tabEndValue);
        slideTab.setDuration(1000);
        //slideViewPager.setDuration(1000);
        slideTab.start();
        //slideViewPager.start();

        isTabVisible = !isTabVisible;

    }
}
