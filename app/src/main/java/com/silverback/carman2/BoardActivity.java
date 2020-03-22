package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.fragments.BoardWriteFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.BoardViewModel;
import com.silverback.carman2.viewmodels.ImageViewModel;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/*
 * This activity consists of the appbar component, the framelayout to alternatively contain the
 * viewpager and the fragment to write a post, and the layout to show checkboxes which are used
 * not only as query conditions for the viewpager but also as field values for the fragment.
 * Plus, there is a nested interface, OnFilterCheckBoxListener, which notifies BoardPagerFragment
 * of which checkbox has changed.
 */
public class BoardActivity extends BaseActivity implements
        View.OnClickListener,
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
    private FirebaseFirestore firestore;
    private OnFilterCheckBoxListener mListener;
    private BoardPagerAdapter pagerAdapter;
    private ImageViewModel imageViewModel;
    private BoardViewModel boardModel;
    private Menu menu;

    // UIs
    private TabLayout boardTabLayout;
    private NestedScrollView nestedScrollView;
    private HorizontalScrollView filterLayout;
    private LinearLayout cbLayout;
    private FrameLayout frameLayout;
    private ViewPager boardPager;
    private BoardWriteFragment writePostFragment;
    private ProgressBar pbLoading;
    private FloatingActionButton fabWrite;


    // Fields
    private ArrayList<CharSequence> cbAutoFilter;//having checkbox values for working as autofilter.
    private boolean isGeneral; //check if a post should be uploaded to the general or just auto.
    private String jsonAutoFilter; //auto data saved in SharedPreferences as JSON String.
    private int tabPage;
    private boolean isFilterVisible;


    // Interface for passing the checkbox values to BoardPagerAdapter to update the AutoClub board.
    // Intial values are passed via BoardPagerAdapter.onCheckBoxValueChange()
    public interface OnFilterCheckBoxListener {
        void onCheckBoxValueChange(ArrayList<CharSequence> autofilter);
        void onGeneralPost(boolean b);
    }
    // Set OnFilterCheckBoxListener to be notified of which checkbox is checked or not, on which
    // query by the tab is based.
    public void setAutoFilterListener(OnFilterCheckBoxListener listener) {
        mListener = listener;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        boardModel = new ViewModelProvider(this).get(BoardViewModel.class);
        firestore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.board_toolbar);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        frameLayout = findViewById(R.id.frame_contents);
        AppBarLayout appBar = findViewById(R.id.appBar);
        boardTabLayout = findViewById(R.id.tab_board);
        filterLayout = findViewById(R.id.post_scroll_horizontal);
        cbLayout = findViewById(R.id.linearLayout_autofilter);
        pbLoading = findViewById(R.id.progbar_board_loading);
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
        try {createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
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
        fabWrite.setOnClickListener(this);
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
        this.menu = menu;
        if(frameLayout.getChildAt(0) instanceof ViewPager) {
            if(tabPage == Constants.BOARD_AUTOCLUB) {
                // Retrieve the ci of the auto maker which has feteched in createAutoFilterCheckBox()
                // and set it to the toolbar menu.
                firestore.collection("autodata").whereEqualTo("auto_maker", cbAutoFilter.get(0)).get()
                        .addOnSuccessListener(query -> {
                            for(QueryDocumentSnapshot autoshot : query) {
                                if(autoshot.exists()) {
                                    menu.add(0, MENU_ITEM_FILTER, Menu.NONE, "Filter")
                                            .setIcon(R.drawable.emblem_hyundai)
                                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                                    break;
                                }
                            }
                        });
            } else menu.clear();
        // When the framelayout contains BoardWriteFragment, the toolbar has the upload button
        } else {
            menu.add(0, MENU_ITEM_UPLOAD, Menu.NONE, "UPLOAD")
                    .setIcon(R.drawable.ic_upload)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

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
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Only apply to FAB.
        if(v.getId() != R.id.fab_board_write) return;

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
            // AutoFilter that holds values of the autoclub viewpager has to be eliminated and be
            // ready to have new values in BoardWriteFragment.
            cbAutoFilter.clear();
            try { createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
            catch(JSONException e) {e.printStackTrace();}
            animAutoFilter(true);
        } else animAppbarLayout(true);

        // Invoke onPrepareOptionsMenu() to create menus for the fragment.
        invalidateOptionsMenu();
    }

    // Implement AppBarLayout.OnOffsetChangedListener
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {}

    // Implement ViewPager.OnPageChangeListener which overrides the following 3 overriding methods.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @SuppressWarnings("ConstantConditions")
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
        if(position == Constants.BOARD_AUTOCLUB) {
            invalidateOptionsMenu();

            String maker = cbAutoFilter.get(0).toString();
            String model = cbAutoFilter.get(1).toString();

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            if(!TextUtils.isEmpty(model)) {
                ssb.append(model).append(" ").append(maker).append(" ").append("club");
                int start = model.length() + 1;
                ssb.setSpan(new RelativeSizeSpan(0.5f),
                        start, start + maker.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            } else ssb.append(maker).append(" ").append("club");

            getSupportActionBar().setTitle(ssb);

        } else {
            menu.clear();
            if(position == Constants.BOARD_NOTIFICATION) fabWrite.setVisibility(View.INVISIBLE);
        }
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
                //pbLoading.setVisibility(View.GONE);
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

    // Crete checkboxes in the autofilter dynamically and set initial values and properties.
    private void createAutoFilterCheckBox(Context context, String json, ViewGroup v) throws JSONException {
        // Switch the filter b/w ViewPager containing BoardPagerFragment and BoardWriteFragment.
        if(v.getChildCount() > 0) v.removeAllViews();
        String[] voidTitle = getResources().getStringArray(R.array.board_autofilter_label);

        // Get the auto data from SharedPreferences as JSON string.
        JSONArray jsonAuto = new JSONArray(json);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(15);

        // The autofilter differs in items according to whether the framelayout contains BoardPagerFragment
        // or WriteBoardFragment.
        if(frameLayout.getChildAt(0) == boardPager) {
            TextView tvLabel = new TextView(context);
            tvLabel.setText(getString(R.string.board_filter_title));
            tvLabel.setTypeface(tvLabel.getTypeface(), Typeface.BOLD);
            v.addView(tvLabel, params);

        // If the frame contains WritePostFragment, which means the post writing mode, add the
        // checkbox that indicates that the post appears in any board.
        } else {
            CheckBox cb = new CheckBox(context);
            cb.setText(getString(R.string.board_filter_chkbox_general));
            cb.setTextColor(Color.WHITE);
            cb.setChecked(true);
            isGeneral = true;
            v.addView(cb);
            cb.setOnCheckedChangeListener((chkbox, isChecked) -> {
                log.i("general checked: %s", isChecked);
                //boardModel.getGeneralPost().setValue(cb.isChecked());
                mListener.onGeneralPost(isChecked);
            });
        }

        // Set the names and values to each checkbox with the autodata from SharedPreferences.
        for(int i = 0; i < jsonAuto.length(); i++) {
            CheckBox cb = new CheckBox(context);
            cb.setTextColor(Color.WHITE);

            if(TextUtils.isEmpty(jsonAuto.optString(i))) {
                cb.setEnabled(false);
                cb.setChecked(false);
                //cb.setText(getString(R.string.pref_entry_void));
                cb.setText(voidTitle[i]);
            } else {
                cb.setText(jsonAuto.optString(i));
                if(i == 0 || i == 1) cb.setChecked(true);
                if(i == 0) cb.setEnabled(false);

                if(cb.isChecked()) cbAutoFilter.add(cb.getText());
                // Set the color and value according to a checkbox is checked or not.
                cb.setOnCheckedChangeListener((chkbox, isChecked) -> {
                    if(isChecked) cbAutoFilter.add(chkbox.getText());
                    else cbAutoFilter.remove(chkbox.getText());
                    // referenced in BoardPagerFragment for purpose of requerying posts with new
                    // conditions.
                    mListener.onCheckBoxValueChange(cbAutoFilter);
                    //boardModel.getAutoFilterValues().setValue(cbAutoFilter);
                });
            }

            v.addView(cb, params);
        }
    }

    // Upon completion of uploading a post, remove BoardWriteFragment out of the framelayout, then
    // add the viewpager again with the toolbar menu and title reset.
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

    // Autofilter values referencedd from BoardWriteFragment.
    public ArrayList<CharSequence> getAutoFilterValues() {
        return cbAutoFilter;
    }

    public boolean checkPostGenenral() {
        return isGeneral;
    }

    // Called in BoardPagerAdapter to invoke notifyDataSetChanged() when the checkbox value
    // has changed.
    public BoardPagerAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    // Invoked in BoardPagerAdapter to hold visibility control of the progressbar when post logindg
    // complete and set it to gone in setFirstQuery().
    public ProgressBar getLoadingProgressBar() {
        return pbLoading;
    }

    // Referenced in the child fragments.
    public SharedPreferences getSettings() {
        return mSettings;
    }

    // Referenced in BoardPagerFragment for its vision control as the recyclerview scrolls.
    public FloatingActionButton getFAB() {
        return fabWrite;
    }

}
