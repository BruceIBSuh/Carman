package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.ImageViewModel;
import com.silverback.carman2.utils.Constants;

/**
 * This activity contains a tab-linked viewpager which currently consists of a single fragment shared
 * by 4 pages.
 *
 */
public class BoardActivity extends BaseActivity implements
        CheckBox.OnCheckedChangeListener,
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    // Constants
    public static final int PAGE_RECENT = 0;
    public static final int PAGE_POPULAR = 1;
    public static final int PAGE_AUTOCLUB = 2;
    public static final int PAGE_NOTIFICATION = 3;
    public static final int REQUEST_CODE_CAMERA = 1000;
    public static final int REQUEST_CODE_GALLERY = 1001;
    public static final int MENU_ITEM_FILTER = 1002;

    // Objects
    private OnFilterCheckBoxListener mListener;
    private BoardPagerAdapter pagerAdapter;
    private ImageViewModel imageViewModel;

    // UIs
    private TabLayout boardTabLayout;
    private HorizontalScrollView filterLayout;
    private ViewPager boardPager;
    private ProgressBar pbBoard;
    private CheckBox cbAutoMaker, cbAutoType, cbAutoModel, cbAutoYear;

    // Fields
    private boolean isAutoClub;
    private boolean[] autoclubValues;
    //private boolean isTabVisible;

    // Interface for passing the checkbox values to BoardPagerAdapter to update the AutoClub board.
    // Intial values are passed via the adapter.set
    public interface OnFilterCheckBoxListener {
        void setCheckBoxValues(boolean[] values);
    }

    public void addAutoClubListener(OnFilterCheckBoxListener listener) {
        mListener = listener;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);

        Toolbar toolbar = findViewById(R.id.board_toolbar);
        boardPager = findViewById(R.id.viewpager_board);
        AppBarLayout appBar = findViewById(R.id.appBar);
        boardTabLayout = findViewById(R.id.tab_board);
        filterLayout = findViewById(R.id.post_scroll_horizontal);
        pbBoard = findViewById(R.id.progbar_board);
        cbAutoMaker = findViewById(R.id.chkbox_filter_maker);
        cbAutoType = findViewById(R.id.chkbox_filter_type);
        cbAutoModel = findViewById(R.id.chkbox_filter_model);
        cbAutoYear = findViewById(R.id.chkbox_filter_year);

        // Set Toolbar and its title as AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.billboard_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cbAutoMaker.setText(mSettings.getString(Constants.AUTO_MAKER, "AutoMaker"));
        cbAutoType.setText(mSettings.getString(Constants.AUTO_TYPE, "AutoType"));
        cbAutoModel.setText(mSettings.getString(Constants.AUTO_MODEL, "AutoModel"));
        cbAutoYear.setText(mSettings.getString(Constants.AUTO_YEAR, "AutoYear"));

        // CheckBox values as to auto data which shows in the filter layout for purposes of querying
        // the posting items. The values should be transferred to the adapter which, in turn, passsed
        // to the Auto Club page.
        cbAutoMaker.setOnCheckedChangeListener(this);
        cbAutoType.setOnCheckedChangeListener(this);
        cbAutoModel.setOnCheckedChangeListener(this);
        cbAutoYear.setOnCheckedChangeListener(this);

        // Create FragmentStatePagerAdapter with the checkbox values attached as arugments
        autoclubValues = new boolean[]{true, false, false, false};
        pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager());
        pagerAdapter.setCheckBoxValues(autoclubValues);
        boardPager.setAdapter(pagerAdapter);
        boardTabLayout.setupWithViewPager(boardPager);

        addTabIconAndTitle(this, boardTabLayout);
        animSlideTabLayout();

        // Add the listeners to the viewpager and AppbarLayout
        boardPager.addOnPageChangeListener(this);
        appBar.addOnOffsetChangedListener(this);
    }

    // Receive the image uri as a result of startActivityForResult() revoked in BoardWriteDlgFragment,
    // which has an implicit intent to select an image. The uri is, in turn, sent to BoardWriteDlgFragment
    // as LiveData of ImageViewModel for purposes of showing the image in the image span in the
    // content area and adding it to the image list so as to update the recyclerview adapter.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK || data == null) return;
        imageViewModel.getUriFromImageChooser().setValue(data.getData());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        switch(buttonView.getId()) {
            case R.id.chkbox_filter_maker:
                log.i("autoMaker: %s", cbAutoMaker.isChecked());
                autoclubValues[0] = cbAutoMaker.isChecked();
                break;

            case R.id.chkbox_filter_type:
                log.i("autoType: %s", cbAutoType.isChecked());
                autoclubValues[1] = cbAutoType.isChecked();
                break;

            case R.id.chkbox_filter_model:
                log.i("autoModel: %s", cbAutoModel.isChecked());
                autoclubValues[2] = cbAutoModel.isChecked();
                break;

            case R.id.chkbox_filter_year:
                log.i("autoYear: %s", cbAutoYear.isChecked());
                autoclubValues[3] = cbAutoYear.isChecked();
                break;
        }

        pagerAdapter.setCheckBoxValues(autoclubValues);
        mListener.setCheckBoxValues(autoclubValues);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_board_write, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * On Android 3.0 and higher, the options menu is considered to always be open when menu items
     * are presented in the app bar. When an event occurs and you want to perform a menu update,
     * you must call invalidateOptionsMenu() to request that the system call onPrepareOptionsMenu().
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if(isAutoClub) {
            menu.add(0, MENU_ITEM_FILTER, Menu.NONE, "Filter")
                    //.setIcon(R.drawable.logo_gs)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case MENU_ITEM_FILTER:
                animSlideFilterLayout();
                isAutoClub = !isAutoClub;
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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
        // If the value of isAutoClub becomes true, the filter button shows up in the toolbar and
        // pressing the button calls animSlideFilterLayout() to make the filter layout slide down,
        // which is defined in onOptionsItemSelected()
        if(position == Constants.BOARD_AUTOCLUB) isAutoClub = true;
        else {
            isAutoClub = false;
            animSlideFilterLayout();
        }

        // Request the system to call onPrepareOptionsMenu(), which is required for Android 3 and
        // higher.
        invalidateOptionsMenu();
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

    }

    // When BoardPagerFragment is set to page 2 indicating AutoClub, the filter layout that consists
    // of the checkboxes to make the querying the board conditioned slides down to cover the tab
    // menu by clicking the filter button. Clicking the button again or switching the page, it
    // slides up.
    private void animSlideFilterLayout() {
        // Visibillity control
        //int visibility = (isAutoClub)? View.VISIBLE : View.INVISIBLE;
        //filterLayout.setVisibility(visibility);

        ObjectAnimator slideDown = ObjectAnimator.ofFloat(filterLayout, "y", getActionbarHeight());
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(filterLayout, "y", 0);
        slideUp.setDuration(500);
        slideDown.setDuration(500);

        if(isAutoClub) slideDown.start();
        else slideUp.start();

    }

    // Referenced by the child fragments
    public SharedPreferences getSettings() {
        return mSettings;
    }



}
