package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;

import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.fragments.BoardWriteFragment;
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
    public static final int MENU_ITEM_UPLOAD = 1003;

    // Objects
    private OnFilterCheckBoxListener mListener;
    private BoardPagerAdapter pagerAdapter;
    private ImageViewModel imageViewModel;

    // UIs
    private TabLayout boardTabLayout;
    private NestedScrollView nestedScrollView;
    private HorizontalScrollView filterLayout;
    private FrameLayout frameLayout;
    private ViewPager boardPager;
    private BoardWriteFragment writePostFragment;
    private ProgressBar pbBoard;
    private CheckBox cbMaker, cbModel, cbType, cbYear;
    private FloatingActionButton fabWrite;

    // Fields
    private int tabPage;
    private boolean isClubPage;
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
        nestedScrollView = findViewById(R.id.nestedScrollView);
        frameLayout = findViewById(R.id.frame_contents);
        AppBarLayout appBar = findViewById(R.id.appBar);
        boardTabLayout = findViewById(R.id.tab_board);
        filterLayout = findViewById(R.id.post_scroll_horizontal);
        pbBoard = findViewById(R.id.progbar_board);
        cbMaker = findViewById(R.id.chkbox_filter_maker);
        cbType = findViewById(R.id.chkbox_filter_type);
        cbModel = findViewById(R.id.chkbox_filter_model);
        cbYear = findViewById(R.id.chkbox_filter_year);
        fabWrite = findViewById(R.id.fab_board_write);

        // Set Toolbar and its title as AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.billboard_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /*
        cbMaker.setText(mSettings.getString(Constants.AUTO_MAKER, "AutoMaker"));
        cbType.setText(mSettings.getString(Constants.AUTO_TYPE, "AutoType"));
        cbModel.setText(mSettings.getString(Constants.AUTO_MODEL, "AutoModel"));
        cbYear.setText(mSettings.getString(Constants.AUTO_YEAR, "AutoYear"));
        */
        handleFilterBar();
        // CheckBox values as to auto data which shows in the filter layout for purposes of querying
        // the posting items. The values should be transferred to the adapter which, in turn, passsed
        // to the Auto Club page.
        cbMaker.setOnCheckedChangeListener(this);
        cbType.setOnCheckedChangeListener(this);
        cbModel.setOnCheckedChangeListener(this);
        cbYear.setOnCheckedChangeListener(this);

        // Create FragmentStatePagerAdapter with the checkbox values attached as arugments
        autoclubValues = new boolean[]{true, false, false, false};
        pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager());
        pagerAdapter.setCheckBoxValues(autoclubValues);

        // Create ViewPager programmatically setting the listener to be notified of the page change
        // and add it to FrameLayout
        boardPager = new ViewPager(this);
        boardPager.setId(View.generateViewId());

        boardPager.setAdapter(pagerAdapter);
        boardTabLayout.setupWithViewPager(boardPager);
        boardPager.addOnPageChangeListener(this);
        frameLayout.addView(boardPager);

        log.i("Frame childview: %s", frameLayout.getChildAt(0));

        addTabIconAndTitle(this, boardTabLayout);
        animSlideTabLayout();

        // Add the listeners to the viewpager and AppbarLayout
        appBar.addOnOffsetChangedListener(this);

        // Set the click listener to the FAB
        fabWrite.setSize(FloatingActionButton.SIZE_AUTO);
        fabWrite.setOnClickListener(view -> {
            // Handle the toolbar menu as the write board comes in.
            fabWrite.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle("Write Your Car");
            // When tapping the fab on the auto club page in the viewpager, slide up the tablayout
            // and sequentially slide down the filterlayout. Whereas, unless the current page is
            // the club one, slide up the tablayout and slide up the nestedscrollview at the same
            // time.
            if(isClubPage) animSlideFilterLayout(true);
            else animWritingBoardLayout(true);

            //if(tabPage != 2) animWritingBoardLayout(true);
            //else animSlideFilterLayout(true);

            // The dialog covers the full screen by adding it in android.R.id.content.
            writePostFragment = new BoardWriteFragment();
            if(frameLayout.getChildCount() > 0) frameLayout.removeAllViews();
            getSupportFragmentManager().beginTransaction().addToBackStack(null)
                    .replace(frameLayout.getId(), writePostFragment)
                    .commit();

            invalidateOptionsMenu();
            log.i("Frame childview: %s", frameLayout.getChildCount());



        });
    }

    // Receive the image uri as a result of startActivityForResult() revoked in BoardWriteFragment,
    // which has an implicit intent to select an image. The uri is, in turn, sent to BoardWriteFragment
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
                log.i("autoMaker: %s", cbMaker.isChecked());
                autoclubValues[0] = cbMaker.isChecked();
                break;

            case R.id.chkbox_filter_type:
                log.i("autoType: %s", cbType.isChecked());
                autoclubValues[1] = cbType.isChecked();
                break;

            case R.id.chkbox_filter_model:
                log.i("autoModel: %s", cbModel.isChecked());
                autoclubValues[2] = cbModel.isChecked();
                break;

            case R.id.chkbox_filter_year:
                log.i("autoYear: %s", cbYear.isChecked());
                autoclubValues[3] = cbYear.isChecked();
                break;
        }

        pagerAdapter.setCheckBoxValues(autoclubValues);
        mListener.setCheckBoxValues(autoclubValues);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_board, menu);
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

        if(frameLayout.getChildAt(0) instanceof ViewPager) {

            if(isClubPage)
                menu.add(0, MENU_ITEM_FILTER, Menu.NONE, "Filter")
                        .setIcon(R.drawable.ic_filter)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);


        } else {
            if(isClubPage) {
                menu.add(0, MENU_ITEM_FILTER, Menu.NONE, "Filter")
                        .setIcon(R.drawable.ic_filter)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                menu.add(0, MENU_ITEM_UPLOAD, Menu.NONE, "UPLOAD")
                        .setIcon(R.drawable.ic_upload)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else {
                menu.add(0, MENU_ITEM_UPLOAD, Menu.NONE, "UPLOAD")
                        .setIcon(R.drawable.ic_upload)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case android.R.id.home:
                // Check which child view the framelayout contains; if it holds the viewpager, just
                // finish the activity and otherwise, add the viewpager to the framelayout.
                if(frameLayout.getChildAt(0) instanceof ViewPager) {
                    frameLayout.removeAllViews();
                    finish();

                } else {
                    frameLayout.removeView(writePostFragment.getView());
                    frameLayout.addView(boardPager);

                    fabWrite.setVisibility(View.VISIBLE);
                    invalidateOptionsMenu();

                    getSupportActionBar().setTitle(getString(R.string.billboard_title));
                    animWritingBoardLayout(false);

                }

                return true;

            case MENU_ITEM_FILTER:
                isClubPage = !isClubPage;
                animSlideFilterLayout(isClubPage);
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
        tabPage = position;
        // If the value of isClubPage becomes true, the filter button shows up in the toolbar and
        // pressing the button calls animSlideFilterLayout() to make the filter layout slide down,
        // which is defined in onOptionsItemSelected()
        isClubPage = (position == Constants.BOARD_AUTOCLUB);
        animSlideFilterLayout(isClubPage);

        // Request the system to call onPrepareOptionsMenu(), which is required for Android 3 and
        // higher.
        invalidateOptionsMenu();
    }
    @Override
    public void onPageScrollStateChanged(int state) {}


    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {

        ObjectAnimator slideTabDown = ObjectAnimator.ofFloat(boardTabLayout, "y", getActionbarHeight());
        ObjectAnimator slideTabUp = ObjectAnimator.ofFloat(boardTabLayout, "y", 0);

        slideTabDown.setDuration(500);
        slideTabUp.setDuration(500);
        slideTabDown.start();
        /*
        if(isVisible) slideTabDown.start();
        else slideTabUp.start();
        */
        // Upon completion of sliding down the tab, set the visibility of the viewpager and the
        // progressbar.
        slideTabDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                boardPager.setVisibility(View.VISIBLE);
                pbBoard.setVisibility(View.GONE);
            }
        });

    }

    // When BoardPagerFragment is set to page 2 indicating AutoClub, the filter layout that consists
    // of the checkboxes to make the querying the board conditioned slides down to cover the tab
    // menu by clicking the filter button. Clicking the button again or switching the page, it
    // slides up.
    private void animSlideFilterLayout(boolean isVisible) {
        // Visibillity control
        //int visibility = (isClubPage)? View.VISIBLE : View.INVISIBLE;
        //filterLayout.setVisibility(visibility);

        ObjectAnimator slideDown = ObjectAnimator.ofFloat(filterLayout, "y", getActionbarHeight());
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(filterLayout, "y", 0);
        slideUp.setDuration(500);
        slideDown.setDuration(500);

        if(isVisible) slideDown.start();
        else slideUp.start();

    }

    private void animWritingBoardLayout(boolean isUpDown) {

        float tabEnd = (isUpDown)? 0 : getActionbarHeight();
        float nestedEnd = (isUpDown)? getActionbarHeight() : getActionbarHeight() + boardTabLayout.getHeight();

        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator slideTabUp = ObjectAnimator.ofFloat(boardTabLayout, "y", tabEnd);
        ObjectAnimator slideNestedUp = ObjectAnimator.ofFloat(nestedScrollView, "y", nestedEnd);

        slideTabUp.setDuration(500);
        slideNestedUp.setDuration(500);

        if(isUpDown) animSet.play(slideTabUp).before(slideNestedUp);
        else animSet.play(slideNestedUp).before(slideTabUp);

        animSet.start();
        //slideTabUp.start();
        //slideNestedUp.start();
    }


    private void handleFilterBar() {
        String brand = mSettings.getString(Constants.AUTO_MAKER, null);
        String model = mSettings.getString(Constants.AUTO_MODEL, null);
        String type = mSettings.getString(Constants.AUTO_TYPE, null);
        String year = mSettings.getString(Constants.AUTO_YEAR, null);
        log.i("auto data: %s, %s, %s, %s", brand, model, type, year);

        if(TextUtils.isEmpty(brand)) {
            cbMaker.setText(getString(R.string.board_filter_brand));
            cbMaker.setEnabled(false);
        } else cbMaker.setText(brand);

        if(TextUtils.isEmpty(model)) {
            cbModel.setText(getString(R.string.board_filter_model));
            cbModel.setEnabled(false);
        } else cbModel.setText(model);

        if(TextUtils.isEmpty(type)) {
            cbType.setText(getString(R.string.board_filter_type));
            cbType.setEnabled(false);
        } else cbType.setText(type);

        if(TextUtils.isEmpty(year)) {
            cbYear.setText(R.string.board_filter_year);
            cbYear.setEnabled(false);
        } else cbYear.setText(year);


    }

    // Referenced by the child fragments
    public SharedPreferences getSettings() {
        return mSettings;
    }

    public FloatingActionButton getFAB() {
        return fabWrite;
    }



}
