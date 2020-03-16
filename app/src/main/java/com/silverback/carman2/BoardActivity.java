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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.fragments.BoardWriteFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.ImageViewModel;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * This activity contains the framelayout which has
 * Take a special attention to the fields of tabPage and isFilterVislbe.
 */
public class BoardActivity extends BaseActivity implements
        CheckBox.OnCheckedChangeListener,
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    // Constants
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
    private boolean isFilterVisible;
    private boolean[] cbFilters;
    private String jsonAutoFilters;
    //private List<Boolean> cbFilters;


    // Interface for passing the checkbox values to BoardPagerAdapter to update the AutoClub board.
    // Intial values are passed via BoardPagerAdapter.onCheckBoxValueChange()
    public interface OnFilterCheckBoxListener {
        void onCheckBoxValueChange(boolean[] values);
    }

    // Set OnFilterCheckBoxListener to be notified of which checkbox is checked or not, on which
    // query by the tab is based.
    public void addAutoFilterListener(OnFilterCheckBoxListener listener) {
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

        // CheckBox values as to auto data which shows in the filter layout for purposes of querying
        // the posting items. The values should be transferred to the adapter which, in turn, passsed
        // to the Auto Club page.
        cbMaker.setOnCheckedChangeListener(this);
        cbType.setOnCheckedChangeListener(this);
        cbModel.setOnCheckedChangeListener(this);
        cbYear.setOnCheckedChangeListener(this);

        // Create FragmentStatePagerAdapter with the checkbox values attached as arugments
        // Set the names and initial values of the checkboxes
        cbFilters = new boolean[4];
        String filterName = setCheckBoxDefaultValues();
        pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager());
        pagerAdapter.setCheckBoxValues(filterName, cbFilters);

        // Create ViewPager with visibility as invisible which turns visible immediately after the
        // animation completes to lesson an amount of workload to query posting items and set
        // the viewpager adapter.
        boardPager = new ViewPager(this);
        boardPager.setId(View.generateViewId());
        //boardPager.setVisibility(View.INVISIBLE);
        boardPager.setVisibility(View.GONE);

        boardPager.setAdapter(pagerAdapter);
        boardTabLayout.setupWithViewPager(boardPager);
        boardPager.addOnPageChangeListener(this);
        frameLayout.addView(boardPager);

        addTabIconAndTitle(this, boardTabLayout);
        animTabLayout(true);

        // Add the listeners to the viewpager and AppbarLayout
        appBar.addOnOffsetChangedListener(this);

        // FAB tapping creates BoardWriteFragment in the framelayout
        fabWrite.setSize(FloatingActionButton.SIZE_AUTO);
        fabWrite.setOnClickListener(view -> {
            // Check if users have made a user name(id). Otherwise, show tne message for setting the
            // user name first.
            String userId = getUserIdFromStorage(this);
            if(TextUtils.isEmpty(userId)) {
                Snackbar.make(nestedScrollView, getString(R.string.board_msg_no_username), Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Handle the toolbar menu as the write board comes in.
            getSupportActionBar().setTitle("Write Your Car");
            fabWrite.setVisibility(View.INVISIBLE);
            // Tapping the fab right when the viewpager holds the auto club page, animate to slide
            // the tablayout up to hide and slide the auto filter down to show sequentially. On the
            // other pages, animation is made to slide up not only the tablayout but also tne
            // nestedscrollview
            if(tabPage == Constants.BOARD_AUTOCLUB) animAutoFilter(true);
            else animAppbarLayout(true);

            // Create the fragment with the user id attached. Remove any view in the framelayout
            // first and put the fragment into it.
            writePostFragment = new BoardWriteFragment();
            Bundle args = new Bundle();
            args.putString("userId", userId);
            args.putInt("tabPage", tabPage);
            args.putString("autoData", mSettings.getString(Constants.AUTO_DATA, null));
            writePostFragment.setArguments(args);

            if(frameLayout.getChildCount() > 0) frameLayout.removeView(boardPager);
            getSupportFragmentManager().beginTransaction().addToBackStack(null)
                    .replace(frameLayout.getId(), writePostFragment)
                    .commit();

            // Invoke onPrepareOptionsMenu() to create menus for the fragment.
            invalidateOptionsMenu();
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

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_board, menu);
        return super.onCreateOptionsMenu(menu);
    }
     */

    /*
     * On Android 3.0 and higher, the options menu is considered to always be open when menu items
     * are presented in the app bar. When an event occurs and you want to make a menu update,
     * you must call invalidateOptionsMenu() to request that the system call onPrepareOptionsMenu().
     */
    //
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(frameLayout.getChildAt(0) instanceof ViewPager) {
            if(tabPage == Constants.BOARD_AUTOCLUB) {
                menu.add(0, MENU_ITEM_FILTER, Menu.NONE, "Filter")
                        .setIcon(R.drawable.ic_filter)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else menu.clear();
        } else {
            menu.add(0, MENU_ITEM_UPLOAD, Menu.NONE, "UPLOAD")
                    .setIcon(R.drawable.ic_upload)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        /*
        // Initially, the framelayout contains the viewpager, which has no menu except that the page
        // indicates the auto club.
        if(frameLayout.getChildAt(0) instanceof ViewPager) {
            if(tabPage == Constants.BOARD_AUTOCLUB) {
                menu.add(0, MENU_ITEM_FILTER, Menu.NONE, "Filter")
                        .setIcon(R.drawable.ic_filter)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        // When tapping the fab, the framelayout contains the fragment to write a post, in which menu
        // varies according to which page the viewpager resides in when creating the fragment. If it
        // is the auto club, menu cosists of the
        // filter and
        } else {
            if(tabPage == Constants.BOARD_AUTOCLUB) {
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

         */

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case android.R.id.home:
                // Check which child view the framelayout contains; if it holds the viewpager, just
                // finish the activity and otherwise, add the viewpager to the framelayout.
                if(frameLayout.getChildAt(0) instanceof ViewPager) {
                    frameLayout.removeAllViews();
                    finish();

                } else if(frameLayout.getChildAt(0) == writePostFragment.getView()){
                    addViewPager();
                }

                return true;

            case MENU_ITEM_FILTER:
                isFilterVisible = !isFilterVisible;
                animAutoFilter(isFilterVisible);
                return true;

            case MENU_ITEM_UPLOAD:
                writePostFragment.initUploadPost();
                //pbFragment = new ProgbarDialogFragment();
                //pbFragment.setProgressMsg("uploading");
                //getSupportFragmentManager().beginTransaction().replace(android.R.id.content, pbFragment).commit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        log.i("onCheckedChanged");
        switch(buttonView.getId()) {
            case R.id.chkbox_filter_maker:
                log.i("autoMaker: %s", cbMaker.isChecked());
                cbFilters[0] = cbMaker.isChecked();
                break;

            case R.id.chkbox_filter_model:
                log.i("autoModel: %s", cbModel.isChecked());
                cbFilters[1] = cbModel.isChecked();

                break;

            case R.id.chkbox_filter_type:
                log.i("autoType: %s", cbType.isChecked());
                cbFilters[2] = cbType.isChecked();
                break;

            case R.id.chkbox_filter_year:
                log.i("autoYear: %s", cbYear.isChecked());
                cbFilters[3] = cbYear.isChecked();
                break;
        }

        mListener.onCheckBoxValueChange(cbFilters);
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
        // If the value of isFilterVisible becomes true, the filter button shows up in the toolbar and
        // pressing the button calls animAutoFilter() to make the filter layout slide down,
        // which is defined in onOptionsItemSelected()
        isFilterVisible = (position == Constants.BOARD_AUTOCLUB);
        animAutoFilter(isFilterVisible);

        // Request the system to call onPrepareOptionsMenu(), which is required for Android 3 and
        // higher.
        if(position == Constants.BOARD_AUTOCLUB) invalidateOptionsMenu();
    }
    @Override
    public void onPageScrollStateChanged(int state) {}


    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animTabLayout(boolean isVisible) {

        ObjectAnimator slideTabDown = ObjectAnimator.ofFloat(boardTabLayout, "y", getActionbarHeight());
        ObjectAnimator slideTabUp = ObjectAnimator.ofFloat(boardTabLayout, "y", 0);

        slideTabDown.setDuration(1000);
        slideTabUp.setDuration(1000);
        //slideTabDown.start();
        if(isVisible) slideTabDown.start();
        else slideTabUp.start();

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
    private void animAutoFilter(boolean isVisible) {
        // Visibillity control
        //int visibility = (isFilterVisible)? View.VISIBLE : View.INVISIBLE;
        //filterLayout.setVisibility(visibility);

        ObjectAnimator slideDown = ObjectAnimator.ofFloat(filterLayout, "y", getActionbarHeight());
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(filterLayout, "y", 0);
        slideUp.setDuration(500);
        slideDown.setDuration(500);

        if(isVisible) slideDown.start();
        else slideUp.start();

    }

    private void animAppbarLayout(boolean isUpDown) {

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

    // Set the checkbox titles and values to each checkbox and put them in List<Boolean>.
    private String setCheckBoxDefaultValues() {

        String brand = mSettings.getString(Constants.AUTO_MAKER, null);
        String model = mSettings.getString(Constants.AUTO_MODEL, null);
        String type = mSettings.getString(Constants.AUTO_TYPE, null);
        String year = mSettings.getString(Constants.AUTO_YEAR, null);

        List<String> autoList = new ArrayList<>();

        if(TextUtils.isEmpty(brand)) {
            cbMaker.setText(getString(R.string.board_filter_brand));
            cbMaker.setEnabled(false);
        } else {
            cbMaker.setText(brand);
            cbFilters[0] = cbMaker.isChecked();
            autoList.add(brand);
        }

        if(TextUtils.isEmpty(model)) {
            cbModel.setText(getString(R.string.board_filter_model));
            cbModel.setEnabled(false);
        } else {
            cbModel.setText(model);
            cbFilters[1] = cbModel.isChecked();
            autoList.add(model);
        }

        if(TextUtils.isEmpty(type)) {
            cbType.setText(getString(R.string.board_filter_type));
            cbType.setEnabled(false);
        } else {
            cbType.setText(type);
            cbFilters[2] = cbType.isChecked();
            autoList.add(type);
        }

        if(TextUtils.isEmpty(year)) {
            cbYear.setText(R.string.board_filter_year);
            cbYear.setEnabled(false);
        } else {
            cbYear.setText(year);
            cbFilters[3] = cbType.isChecked();
            autoList.add(year);
        }

        return new JSONArray(autoList).toString();
    }

    // Upon completion of uploading a post, remove BoardWriteFragment out of the framelayout, then
    // add the viewpager again with the toolbar menu and title re
    @SuppressWarnings("ConstantConditions")
    public void addViewPager() {
        // If any view exists in the framelayout, remove all views out of the layout and add the
        // viewpager.
        if(frameLayout.getChildCount() > 0) frameLayout.removeAllViews();
        frameLayout.addView(boardPager);

        fabWrite.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(getString(R.string.billboard_title));
        invalidateOptionsMenu();

        // Animations differes according to whether the current page is on the auto club or not.
        if(tabPage == Constants.BOARD_AUTOCLUB) {
            animAutoFilter(false);
            isFilterVisible = !isFilterVisible;

        } else animAppbarLayout(false);
    }

    // Referenced by the child fragments
    public SharedPreferences getSettings() {
        return mSettings;
    }

    // Referenced in BoardPagerFragment to show or hide the button as the recyclerview scrolls.
    public FloatingActionButton getFAB() {
        return fabWrite;
    }

    // Get the filter checkbox values
    public boolean[] getCheckBoxValues() {
        return cbFilters;
    }

}
