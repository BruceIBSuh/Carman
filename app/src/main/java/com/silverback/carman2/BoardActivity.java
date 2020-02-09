package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class BoardActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    // Objects
    private TabLayout boardTabLayout;
    private ViewPager boardPager;
    private FloatingActionButton fabWrite;
    private ProgressBar pbBoard;



    // Fields
    //private boolean isTabVisible;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        Toolbar toolbar = findViewById(R.id.board_toolbar);
        //FrameLayout framePager = findViewById(R.id.frame_pager_board);
        boardPager = findViewById(R.id.viewpager_board);
        AppBarLayout appBar = findViewById(R.id.appBar);
        boardTabLayout = findViewById(R.id.tab_board);
        //fabWrite = findViewById(R.id.fab_write);
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

        // Floating Action Button
        /*
        fabWrite.setSize(FloatingActionButton.SIZE_AUTO);
        fabWrite.setOnClickListener(view -> {
            // Initialize the model to prevent getImageObserver() in BoardWriteDlgFragment from
            // automatically invoking startActivityForResult() when the fragment pops up.
            FragmentSharedModel model = new ViewModelProvider(this).get(FragmentSharedModel.class);
            model.getImageChooser().setValue(-1);
            // Pop up the dialog to write the post.
            BoardWriteDlgFragment writePostFragment = new BoardWriteDlgFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, writePostFragment)
                    .commit();

        });
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_options_board, menu);
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
        slideTab.setDuration(1000);
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

    // Turn the floating action button on and off. doubtful if the usage is right!!!!!
    public void handleFabVisibility() {
        //CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams)fabWrite.getLayoutParams();
        //p.setAnchorId(View.NO_ID);
        //fabWrite.setLayoutParams(p);
        if(fabWrite.isOrWillBeShown()) fabWrite.hide();
        else if(fabWrite.isOrWillBeHidden()) fabWrite.show();

    }


}
