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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.fragments.BoardEditFragment;
import com.silverback.carman2.fragments.BoardReadDlgFragment;
import com.silverback.carman2.fragments.BoardWriteFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.BoardViewModel;
import com.silverback.carman2.viewmodels.ImageViewModel;

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
        AppBarLayout.OnOffsetChangedListener, BoardReadDlgFragment.OnEditModeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    // Objects
    private FirebaseFirestore firestore;
    private OnFilterCheckBoxListener mListener;
    private BoardPagerAdapter pagerAdapter;
    private ImageViewModel imgModel;
    private BoardViewModel boardModel;
    private ApplyImageResourceUtil imgResUtil;
    private Menu menu;
    private MenuItem menuEmblem;
    private Drawable emblemIcon;
    private BoardReadDlgFragment readFragment;

    // UIs
    private CoordinatorLayout coordinatorLayout;
    private TabLayout boardTabLayout;
    private Toolbar toolbar;
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

        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        boardModel = new ViewModelProvider(this).get(BoardViewModel.class);
        firestore = FirebaseFirestore.getInstance();
        imgResUtil = new ApplyImageResourceUtil(this);

        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        toolbar = findViewById(R.id.board_toolbar);
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
        getSupportActionBar().setTitle(getString(R.string.board_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tabPage = Constants.BOARD_RECENT;

        // The reason to use cbAutoFilter as ArrrayList<CharSequence> is that the list goes to the
        // singleton of BoardPagerFragment, which should be passed to onCreate() of the same fragment
        // as Bundle.putCharSequenceArrayList().
        cbAutoFilter = new ArrayList<>();

        jsonAutoFilter = mSettings.getString(Constants.AUTO_DATA, null);
        try {createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
        catch (JSONException e) {e.printStackTrace();}

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

        //addTabIconAndTitle(this, boardTabLayout);
        animTabLayout(true);

        // Add the listeners to the viewpager and AppbarLayout
        appBar.addOnOffsetChangedListener(this);

        // FAB tapping creates BoardWriteFragment in the framelayout
        fabWrite.setSize(FloatingActionButton.SIZE_AUTO);
        fabWrite.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        addTabIconAndTitle(this, boardTabLayout);
    }

    // Attach listeners to the parent activity when a fragment is attached to the parent activity.
    @Override
    public void onAttachFragment(Fragment fragment) {
        if(fragment instanceof BoardReadDlgFragment) {
            readFragment = (BoardReadDlgFragment)fragment;
            readFragment.setEditModeListener(this);
        }
    }



    // Receive the image uri as a result of startActivityForResult() revoked in BoardWriteFragment,
    // which has an implicit intent to select an image. The uri is, in turn, sent to BoardWriteFragment
    // as LiveData of ImageViewModel for purposes of showing the image in the image span in the
    // content area and adding it to the image list so as to update the recyclerview adapter.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK || data == null) return;
        switch(requestCode) {
            case Constants.REQUEST_BOARD_GALLERY:
                imgModel.getUriFromImageChooser().setValue(data.getData());
                break;

            case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:
                String json = data.getStringExtra("jsonAutoData");
                log.i("JSON Auto Data: %s", json);
                jsonAutoFilter = json;

                // Create the autofilter checkboxes and set inital values to the checkboxes
                try{ createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
                catch(JSONException e){e.printStackTrace();}

                // Update the pagerAdapter
                pagerAdapter.setAutoFilterValues(cbAutoFilter);
                pagerAdapter.notifyDataSetChanged();
                boardPager.setAdapter(pagerAdapter);
                boardPager.setCurrentItem(Constants.BOARD_AUTOCLUB, true);

                menu.getItem(0).setVisible(true);
                break;

            default: break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options_board, menu);
        this.menu = menu;

        if(emblemIcon != null) menu.getItem(0).setIcon(emblemIcon);
        // Notified that a drawale is prepared for setting it to the options menu icon by
        // setAutoMakerEmblem()
        imgModel.getGlideDrawableTarget().observe(this, drawable -> {
            emblemIcon = drawable;
            menu.getItem(0).setIcon(drawable);
        });

        //return true;
        return super.onCreateOptionsMenu(menu);
    }

    /*
     * On Android 3.0 and higher, the options menu is considered to always be open when menu items
     * are presented in the app bar. When an event occurs and you want to make a menu update,
     * you must call invalidateOptionsMenu() to request that the system call onPrepareOptionsMenu().
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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

                } else addViewPager();

                return true;

            case R.id.action_automaker_emblem:
                isFilterVisible = !isFilterVisible;
                animAutoFilter(isFilterVisible);
                return true;

            case R.id.action_upload_post:
                writePostFragment.initUploadPost();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Only apply to the floating action button
        if(v.getId() != R.id.fab_board_write) return;

        // Check if users have made a user name. Otherwise, show tne message for setting the name
        // first before writing a post.
        String userName = mSettings.getString(Constants.USER_NAME, null);
        if(TextUtils.isEmpty(userName)) {
            Snackbar snackbar = Snackbar.make(
                    coordinatorLayout, getString(R.string.board_msg_username), Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.board_msg_action_setting, view -> {
                Intent intent = new Intent(BoardActivity.this, SettingPreferenceActivity.class);
                intent.putExtra("requestCode", Constants.REQUEST_BOARD_SETTING_USERNAME);
                startActivityForResult(intent, Constants.REQUEST_BOARD_SETTING_USERNAME);
            });

            snackbar.show();

            return;
        }
        // Handle the toolbar menu as the write board comes in.
        if(tabPage != Constants.BOARD_AUTOCLUB) getSupportActionBar().setTitle("Write Your Car");
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
        //invalidateOptionsMenu();
        menu.getItem(1).setVisible(true);
    }

    // Implement BoardReadDlgFragment.OnEditModeListener when the edit button clicks.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEditClicked(Bundle bundle) {
        log.i("Edit Mode");
        BoardEditFragment editFragment = new BoardEditFragment();
        editFragment.setArguments(bundle);

        if(frameLayout.getChildCount() > 0) frameLayout.removeView(boardPager);
        getSupportFragmentManager().beginTransaction().addToBackStack(null)
                .replace(frameLayout.getId(), editFragment)
                .commit();

        // Manager title, menu, and the fab visibility.
        getSupportActionBar().setTitle("Edit your post");
        menu.getItem(1).setVisible(true);
        fabWrite.setVisibility(View.GONE);
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
        if(position == Constants.BOARD_NOTIFICATION) fabWrite.setVisibility(View.INVISIBLE);
        else fabWrite.setVisibility(View.VISIBLE);

        // Create the club title which varies according to the autoclub scope. If it extends to the
        // auto model, it occupies the fisrt place, then the auto make the next with the font size
        // smaller. At a later time, an animation should be applied here.
        if(position == Constants.BOARD_AUTOCLUB) {
            // Check if the auto data which should be saved in SharedPreferences exists
            //if(TextUtils.isEmpty(jsonAutoFilter)) return;
            if(cbAutoFilter.size() == 0) return;

            // Request the system to call onPrepareOptionsMenu(), which is required for Android 3 and
            // higher and handle the visibility of the fab.
            //invalidateOptionsMenu();
            menu.getItem(0).setVisible(true);

            // Create the toolbar title
            SpannableStringBuilder title = createAutoClubTitle();
            getSupportActionBar().setTitle(title);


        } else {
            getSupportActionBar().setTitle(R.string.board_title);
            menu.getItem(0).setVisible(false);

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

    // Create the toolbar title which depends on which checkbox is checked and is applied only when
    // the viewpager has the auto club page.
    private SpannableStringBuilder createAutoClubTitle() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if(cbAutoFilter.size() >= 2) {
            ssb.append(cbAutoFilter.get(1)).append(" ").append(cbAutoFilter.get(0));
            int start = cbAutoFilter.get(1).length() + 1;
            int end = start + cbAutoFilter.get(0).length();
            ssb.setSpan(new RelativeSizeSpan(0.5f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            ssb.append(cbAutoFilter.get(0));
        }

        ssb.append(String.format("%4s", getString(R.string.board_filter_club)));
        return ssb;
    }


    // Dynamically create checkboxes with a json string which is given from SharedPreferences.
    // If the json is empty or null, show the clickable spanned message to ask users to set the
    // auto data in SettingPreferenceActivit, instead. with the json given, checkboxes differs
    // according to which child the frame contains. BoardWriteFragment adds addition checkbox for
    // whether a post is open not only the autoclub  but also to the general board. Checked values
    // are added to cbAutoFilter, which will be used as a query condition.
    private void createAutoFilterCheckBox(Context context, String json, ViewGroup v) throws JSONException {
        // Switch the filter b/w ViewPager containing BoardPagerFragment and BoardWriteFragment.
        if(v.getChildCount() > 0) v.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(15);

        // With the json string empty, show the spanned message to initiate startActivityForResult()
        // to have users set the auto data in SettingPreferenceActivity.
        if(TextUtils.isEmpty(json)) {
            TextView tvMessage = new TextView(context);
            String msg = getString(R.string.board_filter_join);
            SpannableString ss = new SpannableString(msg);
            ClickableSpan clickableSpan = new ClickableSpan(){
                @Override
                public void onClick(@NonNull View textView) {
                    log.i("ClickableSpan clicked");
                    int requestCode = Constants.REQUEST_BOARD_SETTING_AUTOCLUB;
                    Intent intent = new Intent(BoardActivity.this, SettingPreferenceActivity.class);
                    intent.putExtra("requestCode", requestCode);
                    startActivityForResult(intent, requestCode);
                }
            };

            ss.setSpan(clickableSpan, 7, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvMessage.setText(ss);
            tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
            v.addView(tvMessage, params);

            return;
        }

        // Creat checkboxes with the json string given. When the frame contains BoardWriteFragment,
        // an checkbox is added to ask whehter a post is enlisted in the general board.
        JSONArray jsonAuto = new JSONArray(json);
        if(frameLayout.getChildAt(0) == boardPager) {
            TextView tvLabel = new TextView(context);
            tvLabel.setText(getString(R.string.board_filter_title));
            tvLabel.setTextColor(Color.WHITE);
            tvLabel.setTypeface(tvLabel.getTypeface(), Typeface.BOLD);
            v.addView(tvLabel, params);
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

        // Set names and properties to each checkbox in accordance with whehter an element of the
        // json array is valid or null.
        for(int i = 0; i < jsonAuto.length(); i++) {
            CheckBox cb = new CheckBox(context);
            cb.setTextColor(Color.WHITE);
            if(TextUtils.isEmpty(jsonAuto.optString(i))) {
                cb.setEnabled(false);
                cb.setChecked(false);
                cb.setText(getString(R.string.pref_entry_void));
                //cb.setText(voidTitle[i]);
            } else {
                //log.i("empty label: %s", jsonAuto.optString(i));
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
                    SpannableStringBuilder title = createAutoClubTitle();
                    if(getSupportActionBar() != null) getSupportActionBar().setTitle(title);
                });
            }
            v.addView(cb, params);
        }

        // Once an auto maker is fetched, download the uri string for its emblem.
        if(emblemIcon == null) setAutoMakerEmblem();
    }

    // Attemp to retrieve the emblem uir from Firestore only when an auto maker is provided. For this
    // reason, the method should be placed at the end of createAutoFilterCheckBox() which receives
    // auto data as json type.
    private void setAutoMakerEmblem() {
        firestore.collection("autodata").whereEqualTo("auto_maker", cbAutoFilter.get(0)).get()
                .addOnSuccessListener(query -> {
                    for(QueryDocumentSnapshot autoshot : query) {
                        if(autoshot.exists()) {
                            String emblem = autoshot.getString("emblem");
                            // Empty Check. Refactor should be taken to show an empty icon, instead.
                            if(TextUtils.isEmpty(emblem)) return;
                            else imgResUtil.applyGlideToEmblem(autoshot.getString("emblem"),
                                    Constants.ICON_SIZE_TOOLBAR_EMBLEM, imgModel);
                            break;
                        }
                    }
                });
    }

    // Upon completion of uploading a post in BoardWriteFragment, remove it out of the framelayout,
    // then add the viewpager again with the toolbar menu and title reset.
    @SuppressWarnings("ConstantConditions")
    public void addViewPager() {
        // If any view exists in the framelayout, remove all views out of the layout and add the
        // viewpager.
        if(frameLayout.getChildCount() > 0) frameLayout.removeAllViews();
        frameLayout.addView(boardPager);

        fabWrite.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(getString(R.string.board_title));
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



    // Autofilter values referenced in BoardPagerFragment as well as BoardWriteFragment. The value
    // of whehter a post should be uploaded in the general board is referenced in the same fragments
    // to query or upload posts.
    public ArrayList<CharSequence> getAutoFilterValues() {
        return cbAutoFilter;
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

    // Get the framelayout to contain an post to edit when the edit button of the toolbar on
    // BoardReadDlgFragment clicks.
    public FrameLayout getBoardFrameLayout() {
        return frameLayout;
    }

}
