package com.silverback.carman2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.fragments.BoardEditFragment;
import com.silverback.carman2.fragments.BoardReadDlgFragment;
import com.silverback.carman2.fragments.BoardWriteFragment;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/*
 * This activity consists of the appbar component, the framelayout to alternatively contain the
 * viewpager and the fragment to write or edit a post, and the layout to show dynamically created
 * checkboxes which are used not only as query conditions of each board, but also as field values
 * for each fragment. Plus, there is a nested interface, OnFilterCheckBoxListener, which notifies
 * BoardPagerFragmen of which checkbox has changed.
 */
public class BoardActivity extends BaseActivity implements
        View.OnClickListener,
        CheckBox.OnCheckedChangeListener,
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener,
        BoardReadDlgFragment.OnEditModeListener {

    // Logging
    //private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    // Objects
    private OnAutoFilterCheckBoxListener mListener;
    private BoardPagerAdapter pagerAdapter;
    private Menu menu;
    private BoardWriteFragment writePostFragment;
    private BoardEditFragment editPostFragment;

    // UIs
    private CoordinatorLayout coordinatorLayout;
    private TabLayout boardTabLayout;
    private HorizontalScrollView filterLayout;
    private LinearLayout cbLayout;
    private FrameLayout frameLayout;
    private ViewPager boardPager;
    private ProgressBar pbLoading;
    private FloatingActionButton fabWrite;
    private List<CheckBox> chkboxList;
    private ArrayList<String> cbAutoFilter;//having checkbox values for working as autofilter.
    private TextView tvAutoFilterLabel;
    private CheckBox cbGeneral;

    // Fields
    private boolean isGeneral; //check if a post should be uploaded to the general or just auto.
    private String jsonAutoFilter; //auto data saved in SharedPreferences as JSON String.
    private SpannableStringBuilder clubTitle;
    private int tabHeight;
    private int tabPage;
    private boolean isAutoFilter, isTabHeight, isLocked;

    // Interface to notify BoardPagerFragment that a checkbox value changes, which simultaneously
    // have a query with new conditions to make the recyclerview updated.
    public interface OnAutoFilterCheckBoxListener {
        void onCheckBoxValueChange(ArrayList<String> autofilter);
    }
    // Method to attach the listener in the client.
    public void setAutoFilterListener(OnAutoFilterCheckBoxListener listener) {
        mListener = listener;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        AppBarLayout appBar = findViewById(R.id.appBar);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        Toolbar toolbar = findViewById(R.id.board_toolbar);
        frameLayout = findViewById(R.id.frame_contents);
        boardTabLayout = findViewById(R.id.tab_board);
        filterLayout = findViewById(R.id.post_scroll_horizontal);
        cbLayout = findViewById(R.id.linearLayout_autofilter);
        pbLoading = findViewById(R.id.progbar_board_loading);
        fabWrite = findViewById(R.id.fab_board_write);

        // Set Toolbar and its title as AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.board_general_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tabPage = Constants.BOARD_RECENT;


        // The reason to use cbAutoFilter as ArrrayList<CharSequence> is that the list goes to the
        // singleton of BoardPagerFragment, which should be passed to onCreate() of the same fragment
        // as Bundle.putCharSequenceArrayList().
        chkboxList = new ArrayList<>();
        cbAutoFilter = new ArrayList<>();

        jsonAutoFilter = mSettings.getString(Constants.AUTO_DATA, null);
        try { createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
        catch (JSONException e) {e.printStackTrace();}

        pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager());
        pagerAdapter.setAutoFilterValues(cbAutoFilter);

        // Create ViewPager with visibility as invisible which turns visible immediately after the
        // animation completes to lesson an amount of workload to query posting items and set
        // the viewpager adapter.
        boardPager = new ViewPager(this);
        boardPager.setId(View.generateViewId());
        boardPager.setVisibility(View.GONE);
        boardPager.setAdapter(pagerAdapter);
        boardTabLayout.setupWithViewPager(boardPager);
        boardPager.addOnPageChangeListener(this);

        addTabIconAndTitle(this, boardTabLayout);
        frameLayout.addView(boardPager);

        // Add the listeners to the viewpager and AppbarLayout
        appBar.addOnOffsetChangedListener(this);

        //addTabIconAndTitle(this, boardTabLayout);
        animTabLayout();

        // FAB tapping creates BoardWriteFragment in the framelayout
        fabWrite.setSize(FloatingActionButton.SIZE_AUTO);
        fabWrite.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    // Attach listeners to the parent activity when a fragment is attached to the parent activity.
    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if(fragment instanceof BoardReadDlgFragment) {
            BoardReadDlgFragment readFragment = (BoardReadDlgFragment)fragment;
            readFragment.setEditModeListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_options_board, menu);

        // What's difference b/w return true and super.onCreateOptionsMenu(menu)
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
                    frameLayout.removeView(boardPager);
                    finish();

                // Fragment container may hold either BoardWriteFragment or BoardEditFragment. When
                // pressing the up button, either of the fragments is removed from the container and
                // it shoud be null for garbage collecting it.
                } else {
                    boolean isWriteMode = (writePostFragment != null) &&
                            frameLayout.getChildAt(0) == writePostFragment.getView();
                    String msg = (isWriteMode)? getString(R.string.board_msg_cancel_write) :
                            getString(R.string.board_msg_cancel_edit);

                    Fragment target = (isWriteMode)? writePostFragment : editPostFragment;
                    Snackbar snackBar = Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_LONG);
                    snackBar.setAction("OK", view -> {
                        // Remove BoardWriteFragment when the up button presses.
                        getSupportFragmentManager().beginTransaction().remove(target).commit();
                        // Hide the soft input when BoardWriteFragment disappears
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(coordinatorLayout.getWindowToken(), 0);
                        // As BoardPagerFragment comes in, the tabLayout animates to display and the
                        // menu for uploading is made invisible.
                        animTabHeight(true);
                        menu.getItem(1).setVisible(false);

                        addViewPager();

                    }).show();
                }

                return true;

            case R.id.action_upload_post:
                // Network check. If no network exists, no matter what is wifi or mobile data,
                // show the error message and nothing is done.
                // Refactor: WorkManager should be applied to upload a post as any network is
                // re-established.
                if(!isNetworkConnected) {
                    String errNoNetwork = getString(R.string.error_no_network_upload_fail);
                    Snackbar.make(coordinatorLayout, errNoNetwork, Snackbar.LENGTH_SHORT).show();
                    return false;
                }

                // Make it differencitated that what the current page contains b/w BoardWriteFragment
                // and BoardEditFragment.
                boolean isWriteMode = (writePostFragment != null) &&
                        frameLayout.getChildAt(0) == writePostFragment.getView();
                if(isWriteMode) writePostFragment.prepareAttachedImages();
                else editPostFragment.prepareUpdate();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    // Implement AppBarLayout.OnOffsetChangedListener
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i){}

    // Implement ViewPager.OnPageChangeListener which overrides the following 3 overriding methods.
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onPageSelected(int position){
        tabPage = position;
        menu.getItem(0).setVisible(false);
        fabWrite.setVisibility(View.VISIBLE);
        if(isAutoFilter) animAutoFilter(isAutoFilter);

        switch(position) {
            case Constants.BOARD_RECENT | Constants.BOARD_POPULAR:
                getSupportActionBar().setTitle(getString(R.string.board_general_title));
                break;

            case Constants.BOARD_AUTOCLUB:
                if(cbAutoFilter.size() > 0) {
                    clubTitle = createAutoClubTitle();
                    getSupportActionBar().setTitle(clubTitle);
                } else getSupportActionBar().setTitle(getString(R.string.board_tab_title_autoclub));

                // Set the visibility and the icon to the automaker menu icon.
                //menu.getItem(0).setVisible(true);
                //menu.getItem(0).setIcon(R.drawable.ic_automaker_hyundai); // Refactor required.

                animAutoFilter(isAutoFilter);

                break;

            case Constants.BOARD_NOTIFICATION:
                getSupportActionBar().setTitle(getString(R.string.board_tab_title_notification));
                fabWrite.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {}


    // When clicking the fab to write a post, make BoardEditFragment null if it remains in the frame.
    // Then, check if the user has set a nickname. Otherwise, show the spanned message to guide the
    // user to move SettingPreferenceActivity to create it.
    // The tab layout should be animated to decrease its height to 0 for purpose of adjusing the post
    // content area to the base line.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Only apply to the floating action button
        if(v.getId() != R.id.fab_board_write) return;
        if(editPostFragment != null) editPostFragment = null;
        // Check if the user has made a nickname. Otherwise, show tne message for setting the name
        // first and start SettingPreferenceActivity, the result of which is received in onActivity
        // result().
        String userName = mSettings.getString(Constants.USER_NAME, null);
        if(TextUtils.isEmpty(userName)) {
            Snackbar snackbar = Snackbar.make(
                    coordinatorLayout, getString(R.string.board_msg_username), Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.board_msg_action_setting, view -> {
                Intent intent = new Intent(BoardActivity.this, SettingPrefActivity.class);
                intent.putExtra("requestCode", Constants.REQUEST_BOARD_SETTING_USERNAME);
                startActivityForResult(intent, Constants.REQUEST_BOARD_SETTING_USERNAME);
            }).show();
            //snackbar.show();
            return;
        }


        // TabHeight must be measured here b/c it will be increased to the full size or decreased
        // down to 0 each time the button clicks.
        tabHeight = boardTabLayout.getMeasuredHeight();

        // Create BoardWriteFragment with the user id attached. Remove any view in the framelayout
        // first and put the fragment into it.
        if(frameLayout.getChildCount() > 0) frameLayout.removeView(boardPager);

        writePostFragment = new BoardWriteFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putInt("tabPage", tabPage);
        //args.putString("autoData", mSettings.getString(Constants.AUTO_DATA, null));

        // Handle the toolbar menu as the write board comes in.
        if(tabPage != Constants.BOARD_AUTOCLUB) {
            getSupportActionBar().setTitle(getString(R.string.board_title_write));
            animTabHeight(false); // tab height = 0;

        // Tapping the fab right when the viewpager holds the auto club page, animate to slide
        // the tablayout up to hide and slide the auto filter down to show sequentially. On the
        // other pages, animation is made to slide up not only the tablayout but also tne
        // nestedscrollview
        } else {
            // Vistion control as BoardWriteFragment comes in and the general chkbox should be shown.
            tvAutoFilterLabel.setVisibility(View.GONE);
            cbGeneral.setVisibility(View.VISIBLE);

            if(!isAutoFilter) animAutoFilter(isAutoFilter);
            menu.getItem(0).setVisible(false);
        }

        writePostFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                //.addToBackStack(null)
                .replace(frameLayout.getId(), writePostFragment)
                .commit();


        // Visibility control on menu and fab.
        fabWrite.setVisibility(View.INVISIBLE);
        menu.getItem(1).setVisible(true);
    }

    // Implement BoardReadDlgFragment.OnEditModeListener when the edit button presses.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEditClicked(Bundle bundle) {
        if(writePostFragment != null) writePostFragment = null;
        if(frameLayout.getChildAt(0) instanceof ViewPager) frameLayout.removeView(boardPager);
        editPostFragment = new BoardEditFragment();
        editPostFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction().addToBackStack(null)
                .replace(frameLayout.getId(), editPostFragment)
                .commit();

        // Hide the emblem and set the club title if the current page is autoclub.
        if(tabPage == Constants.BOARD_AUTOCLUB) getSupportActionBar().setTitle(clubTitle);
        else getSupportActionBar().setTitle(getString(R.string.board_title_edit));
        if(menu.getItem(0).isVisible()) menu.getItem(0).setVisible(false);

        // Save the height of BoardTabLayout before its size becoms 0 to enable animTabHeight to be
        // workable as BoardPagerFragment comes in.(may replace getViewTreeObeserver() in onCreate().
        tabHeight = boardTabLayout.getMeasuredHeight();
        animTabHeight(false);
        menu.getItem(1).setVisible(true);
        fabWrite.setVisibility(View.GONE);
    }

    // Receive an image uri as a data of startActivityForResult() invoked either in BoardWriteFragment
    // or BoardEditFragment, which holds an implicit intent to call in the image chooser.  The uri is,
    // in turn, sent to either of the fragments as an livedata of the imageviewmodel for purposes of
    // showing the image span in the post and adding it to the image list so as to update the recyclerview
    // adapter.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK || data == null) return;

        switch(requestCode) {
            case Constants.REQUEST_BOARD_GALLERY:
                if(writePostFragment != null) writePostFragment.setUriFromImageChooser(data.getData());
                else if(editPostFragment != null) editPostFragment.setUriFromImageChooser(data.getData());

                break;
            /*
            case Constants.REQUEST_BOARD_CAMERA:
                break;
            */
            case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:
                jsonAutoFilter = data.getStringExtra("jsonAutoData");
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

    // Inplement CheckBox.OnCheckedChangedListener to notify that a checkbox chagnes its value.
    // CheckBox values will be used for conditions for querying posts. At the same time, the automodel
    // checkbox state will manage to set the toolbar title.
    @Override
    public void onCheckedChanged(CompoundButton chkbox, boolean isChecked) {
        final int index = (int)chkbox.getTag();
        if(isChecked) {
            if(index == 1) cbAutoFilter.add(index, chkbox.getText().toString());
            else cbAutoFilter.add(chkbox.getText().toString());

        } else cbAutoFilter.remove(chkbox.getText().toString());

        //for(String filter : cbAutoFilter) log.i("filter : %s", filter);
        // As far as the automodel checkbox value changes, the toolbar title will be reset using
        // creteAutoClubTitle().
        clubTitle = createAutoClubTitle();
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(clubTitle);

        // Referenced in BoardPagerFragment for purpose of requerying posts with new
        // conditions.
        //if(!isLocked)
        mListener.onCheckBoxValueChange(cbAutoFilter);

        // To enable the autoclub enabled when clicking the autofilter, the viewpager is set to
        // POST_NONE in getItemPosition() of BoardPagerAdapter, which destroys not only the autoclub
        // fragment but also the tab titles. Thus, recreate the title here.
        addTabIconAndTitle(this, boardTabLayout);
    }


    // Slide down the tab as the activity is created.
    private void animTabLayout() {
        //float y = (state)? getActionbarHeight() : 0;
        ObjectAnimator slideTab = ObjectAnimator.ofFloat(boardTabLayout, "y", getActionbarHeight());
        slideTab.setDuration(500);
        slideTab.start();

        // Upon completion of sliding down the tab, set the visibility of the viewpager and the
        // progressbar.
        slideTab.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                boardPager.setVisibility(View.VISIBLE);
            }
        });
    }

    // When calling BoardWriteFragment, the tab layout is gone and the fragment comes in and vice versa.
    // state: true - full height false - 0 height.
    private void animTabHeight(boolean isShown) {
        isTabHeight = isShown;
        ValueAnimator anim = (isShown)?
                ValueAnimator.ofInt(0, tabHeight) : ValueAnimator.ofInt(tabHeight, 0);

        anim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams params = boardTabLayout.getLayoutParams();
            params.height = val;
            boardTabLayout.setLayoutParams(params);
        });
        anim.setDuration(500);
        anim.start();
    }

    /*
     * When BoardPagerFragment is set to page 2 indicating AutoClub, the filter layout that consists
     * menu by clicking the filter button. Clicking the button again or switching the page, it should
     * be switched.
     * @param state: if false, the filter should be visible and vice versa.
     */
    private void animAutoFilter(boolean state) {
        float y = (!state)? getActionbarHeight() : 0;
        ObjectAnimator slideAutoFilter = ObjectAnimator.ofFloat(filterLayout, "y", y);
        slideAutoFilter.setDuration(500);
        slideAutoFilter.start();
        isAutoFilter = !isAutoFilter;
    }

    // Create the toolbar title which depends on which checkbox is checked and is applied only when
    // the viewpager has the auto club page.
    public SpannableStringBuilder createAutoClubTitle() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        if(chkboxList.get(1).isChecked()) {
            ssb.append(chkboxList.get(1).getText()).append(" ").append(chkboxList.get(0).getText());
            for(int i = 2; i < chkboxList.size(); i++) {
                if (chkboxList.get(i).isChecked()) ssb.append(" ").append(chkboxList.get(i).getText());
            }

            int start = cbAutoFilter.get(1).length() + 1;
            int end = ssb.length();
            ssb.setSpan(new RelativeSizeSpan(0.5f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        } else {
            ssb.append(chkboxList.get(0).getText());
        }

        ssb.append(String.format("%4s", getString(R.string.board_filter_club)));
        return ssb;
    }


    // Dynamically create checkboxes based on the auto data saved as a json string in SharedPreferences.
    // If the auto data is empty or null, show the clickable spanned message to ask users to set the
    // auto data in SettingPreferenceActivity. If the the auto data is given, checkboxes differs
    // according to which child fragmet the frame contains. BoardWriteFragment adds a checkbox for
    // whether a post is open not only to the autoclub  but also to the general board. Checked values
    // in the checkboxes are added to cbAutoFilter in order to be used as a query condition.
    private void createAutoFilterCheckBox(Context context, String json, ViewGroup v) throws JSONException {
        // Switch the filter b/w ViewPager containing BoardPagerFragment and BoardWriteFragment.
        if(v.getChildCount() > 0) v.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(10);

        // If no autodata is initially given, show the spanned message to initiate startActivityForResult()
        // to have users set the auto data in SettingPreferenceActivity.
        if(TextUtils.isEmpty(json)) {
            TextView tvMessage = new TextView(context);
            String msg = getString(R.string.board_filter_join);
            SpannableString ss = new SpannableString(msg);
            ClickableSpan clickableSpan = new ClickableSpan(){
                @Override
                public void onClick(@NonNull View textView) {
                    int requestCode = Constants.REQUEST_BOARD_SETTING_AUTOCLUB;
                    Intent intent = new Intent(BoardActivity.this, SettingPrefActivity.class);
                    intent.putExtra("requestCode", requestCode);
                    startActivityForResult(intent, requestCode);
                }
            };

            ss.setSpan(clickableSpan, 7, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvMessage.setText(ss);
            // Required to make ClickableSpan workable.
            tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
            v.addView(tvMessage, params);

            return;
        }

        // Create the header and the general checkbox controlled by setting visibility which depends
        // on which fragment the frame contains; header turns visible in BoardPagerFragment and the
        // the checkbox indicating general post turns visible in BoardWriteFragment and vice versa.

        // Create either the autofilter label in BoardPagerFragment or the general checkbox in
        // BoardWritingFragment
        params.topMargin = 0;
        tvAutoFilterLabel = new TextView(context);
        tvAutoFilterLabel.setText(getString(R.string.board_filter_title));
        tvAutoFilterLabel.setTextColor(Color.WHITE);
        tvAutoFilterLabel.setTypeface(tvAutoFilterLabel.getTypeface(), Typeface.BOLD);
        v.addView(tvAutoFilterLabel, params);

        // Create the general checkbox
        cbGeneral = new CheckBox(context);
        cbGeneral.setVisibility(View.GONE);
        cbGeneral.setText(getString(R.string.board_filter_chkbox_general));
        cbGeneral.setTextColor(Color.WHITE);
        v.addView(cbGeneral, params);
        cbGeneral.setOnCheckedChangeListener((chkbox, isChecked) -> isGeneral = isChecked);


        // Dynamically create the checkboxes. The automaker checkbox should be checked and disabled
        // as default values.
        isLocked = mSettings.getBoolean(Constants.AUTOCLUB_LOCK, false);

        JSONArray jsonAuto = new JSONArray(json);
        for(int i = 0; i < jsonAuto.length(); i++) {
            CheckBox cb = new CheckBox(context);
            cb.setTag(i);
            cb.setTextColor(Color.WHITE);

            if(jsonAuto.optString(i).equals("null")) {
                cb.setEnabled(false);
                switch(i) {
                    case 1: cb.setText(R.string.pref_auto_model);break;
                    case 2: cb.setText(R.string.pref_engine_type);break;
                    case 3: cb.setText(R.string.board_filter_year);break;
                }

            } else {
                // The automaker is a necessary checkbox to be checked as far as jsonAuto has set
                // any checkbox. Other autofilter values depends on whether it is the locked mode
                // which retrieves each values from SharedPreferences.

                cb.setText(jsonAuto.optString(i));
                if(i == 0) {
                    cb.setChecked(true);
                    cb.setEnabled(false);
                } else if(isLocked) {
                    final String key = Constants.AUTOFILTER + i;
                    boolean b = mSettings.getBoolean(key, false);
                    cb.setChecked(b);
                    cb.setEnabled(false);
                }

                // Add the checkbox value to the list if it is checked.
                if(cb.isChecked()) cbAutoFilter.add(cb.getText().toString());
                //for(String filter : cbAutoFilter) log.i("autofilter in order: %s", filter);
                // Set the color and value according to a checkbox is checked or not.
                cb.setOnCheckedChangeListener(this);
            }

            v.addView(cb, params);
            chkboxList.add(cb);
        }

        // Create the autofilter lock and handle the event when clicking the imageview.
        setAutoFilterLock(v, jsonAuto);

        // Once an auto maker is fetched, download the uri string for its emblem.
        //if(emblemIcon == null) setAutoMakerEmblem();
    }

    /*
     * Create ImageView and make action controls when cliicking the view.
     * isLock is saved in SharedPreferences and if isLock is true, the checkboxes in the autofilter
     * turn disabled, retaining their values which have been respectively saved as well. When tapping
     * the imageview, isLock will be set to false, enabling the checkboxes to be ready to check
     * except one that has not been set.
     *
     * @param v parent viewgroup
     * @param jsonAuto CheckBox name in the autofilter
     *
     */
    private void setAutoFilterLock(ViewGroup v, JSONArray jsonAuto) {
        // Dynamically create ImageView to put in at the end of the autofilter.
        ImageView imgView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        imgParams.setMargins(30, 10, 0, 0);

        // Set an initial image according to the value of isLocked saved in SharedPreferences.
        int res = (isLocked)? R.drawable.ic_autofilter_lock : R.drawable.ic_autofilter_unlock;
        imgView.setImageResource(res);
        v.addView(imgView, imgParams);

        imgView.setOnClickListener(view -> {
            isLocked = !isLocked;
            int resource = (isLocked)? R.drawable.ic_autofilter_lock : R.drawable.ic_autofilter_unlock;
            imgView.setImageResource(resource);

            for(int i = 1; i < chkboxList.size(); i++) {
                if(isLocked) {
                    chkboxList.get(i).setEnabled(false);
                    String key = Constants.AUTOFILTER + i;
                    mSettings.edit().putBoolean(key, chkboxList.get(i).isChecked()).apply();

                }else if(!jsonAuto.optString(i).equals("null")) chkboxList.get(i).setEnabled(true);
            }

            mSettings.edit().putBoolean(Constants.AUTOCLUB_LOCK, isLocked).apply();
        });

    }


    // Upon completion of uploading a post in BoardWriteFragment or BoardEditFragment when selecting
    // the upload menu, or upon cancellation of writing or editing a post when selecting the up Button,
    // remove the fragment out of the container, then regain the viewpager with the toolbar menu and
    // title reset. If the current page stays in the auto club, additional measures should be taken.
    // Aysnc issue may occur with FireStore. Thus, this method should be carefully invoked.
    @SuppressWarnings("ConstantConditions")
    public void addViewPager() {
        // If any view exists in the framelayout, remove all views out of the layout and add the
        // viewpager
        if(frameLayout.getChildCount() > 0) frameLayout.removeView(frameLayout.getChildAt(0));
        // If the tabLayout height is 0,  put the height back to the default size.
        if(!isTabHeight) animTabHeight(true);

        frameLayout.addView(boardPager);
        fabWrite.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(getString(R.string.board_general_title));



        // Animations differs according to whether the current page is on the auto club or not.
        if(tabPage == Constants.BOARD_AUTOCLUB) {
            // UI visibillity control when boardPagerFragment comes in.
            tvAutoFilterLabel.setVisibility(View.VISIBLE);
            cbGeneral.setVisibility(View.GONE);

            getSupportActionBar().setTitle(createAutoClubTitle());
            if(!menu.getItem(0).isVisible()) menu.getItem(0).setVisible(true);
            if(menu.getItem(1).isVisible()) menu.getItem(1).setVisible(false);
        }

        //Toast.makeText(this, "Upload Done", Toast.LENGTH_SHORT).show();

    }

    // Referenced either in BoardWriteFragmnet or in BoardEditFragment and notified of which media
    // (camera or gallery) to select in BoardChooserDlgFragment. According to the selected media,
    // startActivityForResult() defined in the parent activity is invoked and the result is notified
    // to the activity and it is, in turn, sent back here by calling
    public void getImageFromChooser(int media) {
        switch(media) {
            case Constants.GALLERY:
                // MULTI-SELECTION: special handling of Samsung phone.
                    /*
                    if(Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                        Intent samsungIntent = new Intent("android.intent.action.MULTIPLE_PICK");
                        samsungIntent.setType("image/*");
                        PackageManager manager = getActivity().getApplicationContext().getPackageManager();
                        List<ResolveInfo> infos = manager.queryIntentActivities(samsungIntent, 0);
                        if(infos.size() > 0){
                            //samsungIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            startActivityForResult(samsungIntent, REQUEST_CODE_SAMSUNG);
                        }
                        // General phones other than SAMSUNG
                    } else {
                        Intent galleryIntent = new Intent();
                        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                        galleryIntent.setType("image/*");
                        //galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
                    }
                    */
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                //galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                // The result should go to the parent activity
                startActivityForResult(galleryIntent, Constants.REQUEST_BOARD_GALLERY);
                break;

            case Constants.CAMERA: // Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Intent cameraChooser = Intent.createChooser(cameraIntent, "Choose camera");

                if(cameraIntent.resolveActivity(getPackageManager()) != null) {
                    //log.i("Camera Intent");
                    startActivityForResult(cameraChooser, Constants.REQUEST_BOARD_CAMERA);
                }
                break;

            default: break;
        }

    }

    // Autofilter values referenced in BoardPagerFragment as well as BoardWriteFragment. The value
    // of whehter a post should be uploaded in the general board is referenced in the same fragments
    // to query or upload posts.
    public ArrayList<String> getAutoFilterValues() {
        return cbAutoFilter;
    }
    public BoardPagerAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    public boolean checkGeneralPost() { return isGeneral; }
    // Referenced in BoardPagerFragment for its vision control as the recyclerview scrolls.
    public FloatingActionButton getFAB() {
        return fabWrite;
    }
    public SpannableStringBuilder getAutoClubTitle() {
        return clubTitle;
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

}
