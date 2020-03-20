package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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

/**
 * This activity contains the framelayout which has
 * Take a special attention to the fields of tabPage and isFilterVislbe.
 */
public class BoardActivity extends BaseActivity implements
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
    private LinearLayout cbLayout;
    private FrameLayout frameLayout;
    private ViewPager boardPager;
    private BoardWriteFragment writePostFragment;
    private ProgressBar pbBoard;
    private FloatingActionButton fabWrite;
    private ArrayList<CharSequence> cbAutoFilter;

    // Fields
    private String jsonAutoFilter;
    private int tabPage;
    private boolean isFilterVisible;


    // Interface for passing the checkbox values to BoardPagerAdapter to update the AutoClub board.
    // Intial values are passed via BoardPagerAdapter.onCheckBoxValueChange()
    public interface OnFilterCheckBoxListener {
        void onCheckBoxValueChange(ArrayList<CharSequence> autofilter);
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
        cbLayout = findViewById(R.id.linearLayout_autofilter);
        pbBoard = findViewById(R.id.progbar_board);
        fabWrite = findViewById(R.id.fab_board_write);

        // Set Toolbar and its title as AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.billboard_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tabPage = Constants.BOARD_RECENT;

        // Get the auto data saved as JSON string in SharedPreferences and create the autofilter
        // checkboxes.
        jsonAutoFilter = mSettings.getString(Constants.AUTO_DATA, null);
        cbAutoFilter = new ArrayList<>();
        try { createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
        catch(JSONException e) {e.printStackTrace();}

        pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager());
        pagerAdapter.setAutoFilterValues(cbAutoFilter);

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
            //String userId = getUserIdFromStorage(this);
            String userName = mSettings.getString(Constants.USER_NAME, null);
            if(TextUtils.isEmpty(userName)) {
                Snackbar.make(nestedScrollView, getString(R.string.board_msg_username), Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Handle the toolbar menu as the write board comes in.
            getSupportActionBar().setTitle("Write Your Car");
            fabWrite.setVisibility(View.INVISIBLE);

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

            // Tapping the fab right when the viewpager holds the auto club page, animate to slide
            // the tablayout up to hide and slide the auto filter down to show sequentially. On the
            // other pages, animation is made to slide up not only the tablayout but also tne
            // nestedscrollview
            if(tabPage == Constants.BOARD_AUTOCLUB) {
                try { createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
                catch(JSONException e) {e.printStackTrace();}
                animAutoFilter(true);

            } else animAppbarLayout(true);


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
        // The toolbar has no menus when the framelayout contains the viewpager except the AutoClub
        // page that has a button for query conditions.
        if(frameLayout.getChildAt(0) instanceof ViewPager) {
            if(tabPage == Constants.BOARD_AUTOCLUB) {
                menu.add(0, MENU_ITEM_FILTER, Menu.NONE, "Filter")
                        .setIcon(R.drawable.ic_filter)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else menu.clear();

        // When the framelayout contains BoardWriteFragment, the toolbar has the upload button
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
                } else if(frameLayout.getChildAt(0) == writePostFragment.getView()) {
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
        // higher and handle the visibility of the fab.
        if(position == Constants.BOARD_AUTOCLUB) invalidateOptionsMenu();
        else if(position == Constants.BOARD_NOTIFICATION) fabWrite.setVisibility(View.INVISIBLE);
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


    private void createAutoFilterCheckBox(Context context, String json, ViewGroup v) throws JSONException {

        if(v.getChildCount() > 0) v.removeAllViews();

        JSONArray jsonAuto = new JSONArray(json);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(15);

        if(frameLayout.getChildAt(0) == boardPager) {
            TextView tvLabel = new TextView(context);
            tvLabel.setText("My Club:");
            tvLabel.setTypeface(tvLabel.getTypeface(), Typeface.BOLD);
            v.addView(tvLabel, params);

        // If the frame contains WritePostFragment, which means the post writing mode, add the
        // checkbox that indicates that the post appears in any board.
        } else {
            CheckBox cb = new CheckBox(context);
            cb.setText("일반");
            cb.setTextColor(Color.WHITE);
            cb.setChecked(true);
            v.addView(cb);
        }

        for(int i = 0; i < jsonAuto.length(); i++) {
            CheckBox cb = new CheckBox(context);
            cb.setTextColor(Color.WHITE);

            if(TextUtils.isEmpty(jsonAuto.optString(i))) {
                cb.setEnabled(false);
                cb.setChecked(false);
                cb.setText(getString(R.string.pref_entry_void));

            } else {
                cb.setText(jsonAuto.optString(i));
                if(i == 0 || i == 1) cb.setChecked(true);
                if(cb.isChecked()) cbAutoFilter.add(cb.getText());

                cb.setOnCheckedChangeListener((chkbox, isChecked) -> {
                    if(isChecked) cbAutoFilter.add(chkbox.getText().toString());
                    else cbAutoFilter.remove(chkbox.getText().toString());
                    mListener.onCheckBoxValueChange(cbAutoFilter);

                });
            }

            v.addView(cb, params);
        }

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

        // Revert the autofilter items.
        try { createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
        catch(JSONException e) {e.printStackTrace();}

        // Animations differes according to whether the current page is on the auto club or not.
        if(tabPage == Constants.BOARD_AUTOCLUB) {
            animAutoFilter(false);
            isFilterVisible = !isFilterVisible;
        } else animAppbarLayout(false);
    }


    // Referenced in BoardPagerFragment to show or hide the button as the recyclerview scrolls.
    public FloatingActionButton getFAB() {
        return fabWrite;
    }

    // Autofilter values referencedd from BoardWriteFragment.
    public ArrayList<CharSequence> getCheckBoxValues() {
        return cbAutoFilter;
    }

    // Referenced by the child fragments
    public SharedPreferences getSettings() {
        return mSettings;
    }


}
