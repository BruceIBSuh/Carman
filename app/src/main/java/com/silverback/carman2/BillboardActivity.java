package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BillboardPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class BillboardActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BillboardActivity.class);

    // Objects
    private AppBarLayout appBar;
    private TabLayout boardTabLayout;
    private ViewPager boardPager;
    private FloatingActionButton fabWrite;

    // Fields
    private boolean isTabVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billboard);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        //FrameLayout framePager = findViewById(R.id.frame_pager_board);
        ViewPager boardPager = findViewById(R.id.viewpager_board);
        appBar = findViewById(R.id.appBar);
        boardTabLayout = findViewById(R.id.tab_board);
        fabWrite = findViewById(R.id.fab_write);

        // Add an listener to AppBarLayout
        appBar.addOnOffsetChangedListener(this);
        fabWrite.setOnClickListener(view -> {
            log.i("Writing activity");
            startActivityForResult(new Intent(this, BoardWriteActivity.class), 1000);

        });

        // Set Toolbar and its title as AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Hello Billboard");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        BillboardPagerAdapter pagerAdapter = new BillboardPagerAdapter(getSupportFragmentManager());
        boardPager.setAdapter(pagerAdapter);
        boardPager.addOnPageChangeListener(this);
        boardTabLayout.setupWithViewPager(boardPager);

        addTabIconAndTitle(this, boardTabLayout);
        animSlideTabLayout();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options_board, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsItemSelected in SettingPreferenceActivity");
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

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(boardTabLayout, "y", tabEndValue);
        //ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frameTop, "translationY", tabEndValue);
        slideTab.setDuration(1000);
        //slideViewPager.setDuration(1000);
        slideTab.start();
        //slideViewPager.start();

        isTabVisible = !isTabVisible;

    }


}
