package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;

public class BoardActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    // Constants
    public static final int REQUEST_CODE_CAMERA = 1000;
    public static final int REQUEST_CODE_GALLERY = 1001;

    // Objects
    private TabLayout boardTabLayout;
    private ViewPager boardPager;
    private ProgressBar pbBoard;
    private ImageViewModel imageViewModel;

    // Fields
    //private boolean isTabVisible;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);

        Toolbar toolbar = findViewById(R.id.board_toolbar);
        //FrameLayout framePager = findViewById(R.id.frame_pager_board);
        boardPager = findViewById(R.id.viewpager_board);
        AppBarLayout appBar = findViewById(R.id.appBar);
        boardTabLayout = findViewById(R.id.tab_board);
        pbBoard = findViewById(R.id.progbar_board);

        // Set Toolbar and its title as AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.billboard_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        BoardPagerAdapter pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager());
        boardPager.setAdapter(pagerAdapter);
        boardTabLayout.setupWithViewPager(boardPager);

        addTabIconAndTitle(this, boardTabLayout);
        animSlideTabLayout();

        // Add the listeners to the viewpager and AppbarLayout
        boardPager.addOnPageChangeListener(this);
        appBar.addOnOffsetChangedListener(this);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK || data == null) return;
        log.i("data from startActivityForResult defined in BoardWriteDlgFragment: %s", data);
        imageViewModel.getUriFromImageChooser().setValue(data.getData());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_board_write, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // AppBarLayout.OnOffsetChangedListener invokes this.
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {}

    // ViewPager.OnPageChangeListener invokes the following 3 overriding methods.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
    @Override
    public void onPageSelected(int position) {
        log.i("ViewPager onPageSelected: %s", position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {
        float toolbarHeight = getActionbarHeight();
        ObjectAnimator slideTab = ObjectAnimator.ofFloat(boardTabLayout, "y", toolbarHeight);
        slideTab.setDuration(500);
        slideTab.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                boardPager.setVisibility(View.VISIBLE);
                pbBoard.setVisibility(View.GONE);
            }
        });
        slideTab.start();
        //isTabVisible = !isTabVisible;
    }

    // Referenced by the child fragments
    public SharedPreferences getSettings() {
        return mSettings;
    }
}
