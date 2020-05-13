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
import android.graphics.drawable.Drawable;
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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
import com.silverback.carman2.viewmodels.ImageViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/*
 * This activity consists of the appbar component, the framelayout to alternatively contain the
 * viewpager and the fragment to write a post, and the layout to show checkboxes which are used
 * not only as query conditions for the viewpager but also as field values for the fragment.
 * Plus, there is a nested interface, OnFilterCheckBoxListener, which notifies BoardPagerFragment
 * of which checkbox has changed.
 */
public class BoardActivity extends BaseActivity implements
        View.OnClickListener, CheckBox.OnCheckedChangeListener,
        ViewPager.OnPageChangeListener,
        AppBarLayout.OnOffsetChangedListener, BoardReadDlgFragment.OnEditModeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);


    // Objects
    private FirebaseFirestore firestore;
    private OnFilterCheckBoxListener mListener;
    private BoardPagerAdapter pagerAdapter;
    private ImageViewModel imgModel;
    private ApplyImageResourceUtil imgResUtil;
    private Menu menu;
    private Drawable emblemIcon;
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


    // Fields
    private ArrayList<CharSequence> cbAutoFilter;//having checkbox values for working as autofilter.
    private boolean isGeneral; //check if a post should be uploaded to the general or just auto.
    private String jsonAutoFilter; //auto data saved in SharedPreferences as JSON String.
    private int tabHeight;
    private int tabPage;
    private boolean isAutoFilter;




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

        firestore = FirebaseFirestore.getInstance();
        imgResUtil = new ApplyImageResourceUtil(this);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);

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
    public void onAttachFragment(@NonNull Fragment fragment) {
        if(fragment instanceof BoardReadDlgFragment) {
            BoardReadDlgFragment readFragment = (BoardReadDlgFragment)fragment;
            readFragment.setEditModeListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options_board, menu);
        this.menu = menu;

        if(tabPage == Constants.BOARD_AUTOCLUB) menu.getItem(0).setIcon(emblemIcon);

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
                log.d("back button clicked");
                // Check which child view the framelayout contains; if it holds the viewpager, just
                // finish the activity and otherwise, add the viewpager to the framelayout.
                if(frameLayout.getChildAt(0) instanceof ViewPager) {
                    //frameLayout.removeAllViews();
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

                        // Control the anim and visibility
                        animTabHeight(false);
                        animTabLayout(true);
                        menu.getItem(1).setVisible(false);

                        addViewPager();

                    });
                    snackBar.show();
                }

                return true;

            case R.id.action_automaker_emblem:
                animAutoFilter(isAutoFilter);

                return true;

            case R.id.action_upload_post:
                boolean isWriteMode = (writePostFragment != null) &&
                        frameLayout.getChildAt(0) == writePostFragment.getView();

                if(isWriteMode) writePostFragment.prepareUpload();
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
        if(isAutoFilter) {
            animAutoFilter(isAutoFilter);

        }

        switch(position) {
            case Constants.BOARD_RECENT | Constants.BOARD_POPULAR:
                getSupportActionBar().setTitle(getString(R.string.board_general_title));
                break;

            case Constants.BOARD_AUTOCLUB:
                if(cbAutoFilter.size() > 0) getSupportActionBar().setTitle(createAutoClubTitle());
                else getSupportActionBar().setTitle(getString(R.string.board_tab_title_autoclub));

                // Set the visibility and the icon to the automaker menu icon.
                menu.getItem(0).setVisible(true);
                menu.getItem(0).setIcon(R.drawable.ic_automaker_hyundai); // Refactor required.

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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {
        // Only apply to the floating action button
        if(v.getId() != R.id.fab_board_write) return;
        if(editPostFragment != null) editPostFragment = null;

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

        // Set the boardTabLayout height for purpose of redrawing the layout with the height when
        // returning from BoardWriteFragment.
        tabHeight = boardTabLayout.getMeasuredHeight();
        log.i("boardTabLayout height: %s", tabHeight);

        // Create BoardWriteFragment with the user id attached. Remove any view in the framelayout
        // first and put the fragment into it.
        writePostFragment = new BoardWriteFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putInt("tabPage", tabPage);
        args.putString("autoData", mSettings.getString(Constants.AUTO_DATA, null));
        writePostFragment.setArguments(args);

        if(frameLayout.getChildCount() > 0) frameLayout.removeView(boardPager);
        getSupportFragmentManager().beginTransaction()
                //.addToBackStack(null)
                .replace(frameLayout.getId(), writePostFragment)
                .commit();

        // Handle the toolbar menu as the write board comes in.
        if(tabPage != Constants.BOARD_AUTOCLUB) {
            getSupportActionBar().setTitle(getString(R.string.board_title_write));
            animTabHeight(true);

            // Tapping the fab right when the viewpager holds the auto club page, animate to slide
            // the tablayout up to hide and slide the auto filter down to show sequentially. On the
            // other pages, animation is made to slide up not only the tablayout but also tne
            // nestedscrollview
        } else {
            // AutoFilter that holds values of the autoclub viewpager has to be eliminated and be
            // ready to have new values in BoardWriteFragment.
            cbAutoFilter.clear();
            try { createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
            catch(JSONException e) {e.printStackTrace();}
            if(!isAutoFilter) animAutoFilter(isAutoFilter);
            menu.getItem(0).setVisible(false);
        }

        // Visibility control on menu and fab.
        fabWrite.setVisibility(View.INVISIBLE);
        menu.getItem(1).setVisible(true);
    }

    // Implement BoardReadDlgFragment.OnEditModeListener when the edit button presses.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEditClicked(Bundle bundle) {
        log.i("Edit Mode");
        if(writePostFragment != null) writePostFragment = null;

        editPostFragment = new BoardEditFragment();
        editPostFragment.setArguments(bundle);

        if(frameLayout.getChildAt(0) instanceof ViewPager) frameLayout.removeView(boardPager);
        getSupportFragmentManager().beginTransaction().addToBackStack(null)
                .replace(frameLayout.getId(), editPostFragment)
                .commit();

        // Manager title, menu, and the fab visibility.
        getSupportActionBar().setTitle(getString(R.string.board_title_edit));
        animTabLayout(false);
        animTabHeight(true);
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
                //imgModel.getUriFromImageChooser().setValue(data.getData());
                log.i("current fragment: %s, %s", frameLayout.getChildCount(), frameLayout.getChildAt(0));
                if(writePostFragment != null)
                    writePostFragment.setUriFromImageChooser(data.getData());
                else if(editPostFragment != null)
                    editPostFragment.setUriFromImageChooser(data.getData());
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

    // Inplement CheckBox.OnCheckedChangedListener to notify that a checkbox chagnes its value.
    // CheckBox values will be used for conditions for querying posts. At the same time, the automodel
    // checkbox state will manage to set the toolbar title.
    @Override
    public void onCheckedChanged(CompoundButton chkbox, boolean isChecked) {
        log.i("onCheckedChagned");
        if(isChecked) cbAutoFilter.add(chkbox.getText());
        else cbAutoFilter.remove(chkbox.getText());

        // Referenced in BoardPagerFragment for purpose of requerying posts with new
        // conditions.
        mListener.onCheckBoxValueChange(cbAutoFilter);

        // As far as the automodel checkbox value changes, the toolbar title will be reset using
        // creteAutoClubTitle().
        if(chkbox == chkboxList.get(1)) {
            SpannableStringBuilder title = createAutoClubTitle();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        }
    }


    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animTabLayout(boolean isTabVisible) {
        float y = (isTabVisible)? getActionbarHeight() : 0;
        ObjectAnimator slideTab = ObjectAnimator.ofFloat(boardTabLayout, "y", y);
        slideTab.setDuration(500);
        slideTab.start();

        // Upon completion of sliding down the tab, set the visibility of the viewpager and the
        // progressbar.
        slideTab.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(isTabVisible) boardPager.setVisibility(View.VISIBLE);
                //pbLoading.setVisibility(View.GONE);
            }
        });
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


    // When calling BoardWriteFragment, the tab layout is gone and the fragment comes in and vice versa.
    //private void animAppbarLayout(boolean isUp) {
    private void animTabHeight(boolean isShown) {
        ValueAnimator anim = (isShown)?ValueAnimator.ofInt(tabHeight, 0) : ValueAnimator.ofInt(0, tabHeight);

        anim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams params = boardTabLayout.getLayoutParams();
            params.height = val;
            boardTabLayout.setLayoutParams(params);
        });
        anim.setDuration(500);
        anim.start();
    }

    // Create the toolbar title which depends on which checkbox is checked and is applied only when
    // the viewpager has the auto club page.
    private SpannableStringBuilder createAutoClubTitle() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        //if(cbAutoFilter.size() >= 2) {
        if(chkboxList.get(1).isChecked()) {
            ssb.append(chkboxList.get(1).getText()).append(" ").append(cbAutoFilter.get(0));
            int start = cbAutoFilter.get(1).length() + 1;
            int end = start + cbAutoFilter.get(0).length();
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
        params.setMarginStart(15);

        // If no autodata is given, show the spanned message to initiate startActivityForResult()
        // to have users set the auto data in SettingPreferenceActivity.
        if(TextUtils.isEmpty(json)) {
            TextView tvMessage = new TextView(context);
            String msg = getString(R.string.board_filter_join);
            SpannableString ss = new SpannableString(msg);
            ClickableSpan clickableSpan = new ClickableSpan(){
                @Override
                public void onClick(@NonNull View textView) {
                    int requestCode = Constants.REQUEST_BOARD_SETTING_AUTOCLUB;
                    Intent intent = new Intent(BoardActivity.this, SettingPreferenceActivity.class);
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

        // Create the header or the general checkbox according to the current page
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
            cb.setChecked(false);
            v.addView(cb);
            cb.setOnCheckedChangeListener((chkbox, isChecked) -> {
                log.i("general checked: %s", isChecked);
                mListener.onGeneralPost(isChecked);
            });
        }

        // Dynamically create the checkboxes. The automaker checkbox should be checked and disabled
        // as default values.
        JSONArray jsonAuto = new JSONArray(json);
        for(int i = 0; i < jsonAuto.length(); i++) {
            CheckBox cb = new CheckBox(context);
            cb.setTextColor(Color.WHITE);
            if(jsonAuto.optString(i).equals("null")) {
                cb.setEnabled(false);
                switch(i) {
                    case 1: cb.setText(R.string.pref_auto_model);break;
                    case 2: cb.setText(R.string.pref_engine_type);break;
                    case 3: cb.setText(R.string.pref_auto_year);break;
                }

            } else {
                // The automaker should be checked and disabled unless it is void.
                cb.setText(jsonAuto.optString(i));
                if(i == 0) {
                    cb.setChecked(true);
                    cb.setEnabled(false);
                } else if (i == 1) {
                    cb.setTag("automodel");
                    cb.setChecked(true);
                }

                // Add the checkbox value to the list if it is checked.
                if(cb.isChecked()) cbAutoFilter.add(cb.getText());

                // Set the color and value according to a checkbox is checked or not.
                cb.setOnCheckedChangeListener(this);

            }

            v.addView(cb, params);
            chkboxList.add(cb);
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

        animTabHeight(false);
        fabWrite.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(getString(R.string.board_general_title));
        //invalidateOptionsMenu();

        // Revert the autofilter items.
        try { createAutoFilterCheckBox(this, jsonAutoFilter, cbLayout);}
        catch(JSONException e) {e.printStackTrace();}

        // Animations differes according to whether the current page is on the auto club or not.
        if(tabPage == Constants.BOARD_AUTOCLUB) {
            getSupportActionBar().setTitle(createAutoClubTitle());
            if(!menu.getItem(0).isVisible()) menu.getItem(0).setVisible(true);
        }
    }

    // Notified of which media(camera or gallery) to select in BoardChooserDlgFragment, according
    // to which startActivityForResult() is invoked by the parent activity and the result will be
    // notified to the activity and it is, in turn, sent back here by calling
    public void getImageFromChooser(int media) {
        switch(media) {
            case 1:
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

            case 2: // Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Intent cameraChooser = Intent.createChooser(cameraIntent, "Choose camera");

                if(cameraIntent.resolveActivity(getPackageManager()) != null) {
                    log.i("Camera Intent");
                    startActivityForResult(cameraChooser, Constants.REQUEST_BOARD_CAMERA);
                }
                break;

            default: break;
        }

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
}
