package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.fragments.BoardWriteDlgFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

public class BoardActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    // Objects
    private AppBarLayout appBar;
    private TabLayout boardTabLayout;
    private FloatingActionButton fabWrite;
    private BoardPagerAdapter pagerAdapter;

    // Fields
    private boolean isTabVisible = false;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_posting);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        //FrameLayout framePager = findViewById(R.id.frame_pager_board);
        ViewPager boardPager = findViewById(R.id.viewpager_board);
        appBar = findViewById(R.id.appBar);
        boardTabLayout = findViewById(R.id.tab_board);
        fabWrite = findViewById(R.id.fab_write);

        // Add an listener to AppBarLayout
        appBar.addOnOffsetChangedListener(this);
        fabWrite.setOnClickListener(view -> {
            // Initialize the model to prevent getImageObserver() in BoardWriteDlgFragment from
            // automatically invoking startActivityForResult() when the fragment pops up.
            FragmentSharedModel model = ViewModelProviders.of(this).get(FragmentSharedModel.class);
            model.getImageChooser().setValue(-1);
            // Pop up the dialog to write the post.
            BoardWriteDlgFragment writePostFragment = new BoardWriteDlgFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, writePostFragment)
                    .commit();

        });

        // Set Toolbar and its title as AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.billboard_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        BoardPagerAdapter pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager());
        boardPager.setAdapter(pagerAdapter);
        boardPager.addOnPageChangeListener(this);
        boardTabLayout.setupWithViewPager(boardPager);

        addTabIconAndTitle(this, boardTabLayout);
        animSlideTabLayout();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_options_board, menu);
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
        float tabEndValue = (!isTabVisible)? toolbarHeight : 0;

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(boardTabLayout, "Y", tabEndValue);
        //ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frameTop, "translationY", tabEndValue);
        slideTab.setDuration(1000);
        //slideViewPager.setDuration(1000);
        slideTab.start();
        //slideViewPager.start();

        isTabVisible = !isTabVisible;

    }

    // Referenced by the child fragments
    public SharedPreferences getSettings() {
        return mSettings;
    }

}
