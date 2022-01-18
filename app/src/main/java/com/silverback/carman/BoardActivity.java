/*
 * Copyright (C) 2012 The Carman Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.silverback.carman;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.PropertyName;
import com.silverback.carman.adapters.BoardPagerAdapter;
import com.silverback.carman.databinding.ActivityBoardBinding;
import com.silverback.carman.fragments.BoardEditFragment;
import com.silverback.carman.fragments.BoardReadDlgFragment;
import com.silverback.carman.fragments.BoardWriteFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/*
 * This activity is mainly composed of a framelayout that alternatively contains either a viewpager
 * or the fragments to edit or write a post.
 *
 * The viewpager has fragments statically created for categorized posting board and controlled by
 * BoardPagerAdapter which extends FragmentStateAdapter.
 *
 * The fragment to write a post(BoardWriteFragment) comes in when clicking the fab, replacing the
 * viewpager the activity contains. The fragment to edit a post(BoardEditFragment) replaces the
 * viewpager in the same way when clicking the edit button.  The edit button turns visible in the
 * toolbar as long as the fragment to read a post(BoardReadDlgFragment) pops up and the post is
 * owned by the user.
 *
 * Communications b/w the fragments are mostly made with the livedata defined in FramentSharedModel.
 * Some cases use interfaces, though.
 *
 * OnAutoFilterCheckBoxListener passes any change of the checkbox values to BoardPagerFragment for
 * dynamically querying posts based on it.  OnEditModeListener defined in BoardReadDlgFragment
 * notifies that the user chooses the edit button to open BoardEditFragment.
 *
 * The toolbar menu should be basically handled in the parent activity but may be controlled by
 * each fragment. Thus, the return boolean value in OnOptionsItemSelected() depends on whether the
 * menu proceed(false) or consume(true).
 */

public class BoardActivity extends BaseActivity implements
        View.OnClickListener,
        CheckBox.OnCheckedChangeListener,
        AppBarLayout.OnOffsetChangedListener,
        BoardReadDlgFragment.OnEditModeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    // Objects
    private ActivityBoardBinding binding;
    private OnAutoFilterCheckBoxListener mListener;
    private BoardPagerAdapter pagerAdapter;
    private Menu menu;
    private BoardWriteFragment writePostFragment;
    private BoardEditFragment editPostFragment;

    // UIs
    private TextView tvAutoFilterLabel;
    private CheckBox cbGeneral;

    // Fields
    private List<CheckBox> chkboxList;
    private ArrayList<String> cbAutoFilter;//having checkbox values for working as autofilter.
    private boolean isGeneral; //check if a post should be uploaded to the general or just auto.
    private String jsonAutoFilter; //auto data saved in SharedPreferences as JSON String.
    private SpannableStringBuilder clubTitle;
    private int tabHeight;
    private int tabPage;
    private boolean isAutoFilter, isTabHeight, isLocked;
    private int category;

    // Custom class to typecast the Firestore array field of post_images to ArrayList<String> used
    // in both BoardPostingAdapter and BoardPagerFragment.
    public static class PostImages {
        @PropertyName("post_images")
        private ArrayList<String> postImageList;
        public PostImages() {
            // Mst have a public no-argument constructor
        }
        public PostImages(ArrayList<String> postImageList) {
            this.postImageList = postImageList;
        }
        @PropertyName("post_images")
        public ArrayList<String> getPostImages() {
            return postImageList;
        }
        @PropertyName("post_images")
        public void setPostImages(ArrayList<String> postImageList) {
            this.postImageList = postImageList;
        }

    }

    // Interface to notify BoardPagerFragment that a checkbox value changes, which simultaneously
    // queries posts with new conditions to make the recyclerview updated.
    public interface OnAutoFilterCheckBoxListener {
        void onCheckBoxValueChange(ArrayList<String> autofilter);
    }
    // Method to attach the listener in the client.
    public void setAutoFilterListener(OnAutoFilterCheckBoxListener listener) {
        mListener = listener;
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::getSettingResultBack);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBoardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.boardToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.board_general_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        category = Constants.BOARD_RECENT;

        // chkboxList is created by whether the autodata is set. cbAutoFilter is created
        // by wheteher each checkbox item is checked.
        chkboxList = new ArrayList<>();
        cbAutoFilter = new ArrayList<>();

        // Create the autofilter checkbox if the user's auto data is set. If null, it catches the
        // exception that calls setNoAutofilterText().
        jsonAutoFilter = mSettings.getString(Constants.AUTO_DATA, null);
        try { createAutoFilterCheckBox(this, jsonAutoFilter, binding.autofilter);}
        catch(NullPointerException e) {setNoAutoFilterText();}
        catch(JSONException e) {e.printStackTrace();}

        // ViewPager2
        pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager(), getLifecycle());
        pagerAdapter.setAutoFilterValues(cbAutoFilter);
        binding.boardPager.setVisibility(View.GONE); //show progressbar unitl the query completes.
        binding.boardPager.setAdapter(pagerAdapter);
        //binding.boardPager.registerOnPageChangeCallback(pagerCallback);

        // TabLayoutMediator which interconnects TabLayout and ViewPager2
        List<String> titles = Arrays.asList(getResources().getStringArray(R.array.board_tab_title));
        new TabLayoutMediator(binding.tabBoard, binding.boardPager, (tab, position) ->
            tab.setText(titles.get(position))
        ).attach();

        // FAB tapping creates BoardWriteFragment in the framelayout
        binding.fabBoardWrite.setSize(FloatingActionButton.SIZE_AUTO);
        binding.fabBoardWrite.setOnClickListener(this);

        // onAttachFragment(childFragment) is deprecated in API 28. Instead, use this defined in
        // JetPack androidx.
        getSupportFragmentManager().addFragmentOnAttachListener((fm, fragment) -> {
            if(fragment instanceof BoardReadDlgFragment) {
                BoardReadDlgFragment readFragment = (BoardReadDlgFragment)fragment;
                readFragment.setEditModeListener(this);
            }
        });

        // Add the listeners to the viewpager and AppbarLayout
        //binding.appBar.addOnOffsetChangedListener((appbar, offset) -> {});
        animTabLayout();


    }

    // Should be defined for the autofilter to property work as long as the activity is resumed
    // by onActivityResult callback.
    @Override
    public void onResume() {
        super.onResume();
        binding.boardPager.registerOnPageChangeCallback(pagerCallback);
    }

    @Override
    public void onPause() {
        binding.boardPager.unregisterOnPageChangeCallback(pagerCallback);
        super.onPause();
    }

    @Override
    public void onStop() {
        //activityResultLauncher.unregister();
        super.onStop();
    }



    /*
     * On Android 3.0 and higher, the options menu is considered to always be open when menu items
     * are presented in the app bar. When an event occurs and you want to make a menu update,
     * you must call invalidateOptionsMenu() to request that the system call onPrepareOptionsMenu().
     *
     * What's difference b/w return true and super.onCreateOptionsMenu(menu) is, if true, no menu
     * items are able to be set in the child.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_board, menu);
        this.menu = menu;
        this.menu.getItem(0).setVisible(false);
        this.menu.getItem(1).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            // Check which child view the framelayout contains; if it contains the viewpager, just
            // finish the activity; otherwise, add the viewpager to the framelayout.
            if(binding.frameContents.getChildAt(0) instanceof ViewPager2) {
                binding.frameContents.removeView(binding.boardPager);
                finish();

            } else {
                // FrameLayout may contain either BoardWriteFragment or BoardEditFragment. On pressing
                // the up button, either of the fragments is removed from the container and add the
                // viewpager.
                boolean isWriteMode = (writePostFragment != null) &&
                        binding.frameContents.getChildAt(0) == writePostFragment.getView();
                String msg = (isWriteMode)? getString(R.string.board_msg_cancel_write) :
                        getString(R.string.board_msg_cancel_edit);

                Fragment target = (isWriteMode) ? writePostFragment : editPostFragment;
                Snackbar snackBar = Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG);
                snackBar.setAction("OK", view -> {
                    getSupportFragmentManager().beginTransaction().remove(target).commit();

                    // Hide the soft input when BoardWriteFragment disappears
                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);

                    // As BoardPagerFragment comes in, the tabLayout animates to display and the
                    // menu for uploading is made invisible.
                    animTabHeight(true);

                    //menu.getItem(1).setVisible(false);
                    addViewPager();
                }).show();
            }

            return true;

        } else if(item.getItemId() == R.id.action_upload_post) {
            boolean isWriteMode = (writePostFragment != null) &&
                    binding.frameContents.getChildAt(0) == writePostFragment.getView();
            if(isWriteMode) writePostFragment.prepareAttachedImages();
            else editPostFragment.prepareUpdate();
            return true;

        } else return super.onOptionsItemSelected(item); // means false
    }

    // Implement the abstract class of ViewPager2.OnPageChangeCallback, which has replaced the previous
    // interface type.
    private final ViewPager2.OnPageChangeCallback pagerCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            //tabPage = position;
            //menu.getItem(0).setVisible(false);
            binding.fabBoardWrite.setVisibility(View.VISIBLE);
            animAutoFilter(false);

            switch(position) {
                case Constants.BOARD_RECENT | Constants.BOARD_POPULAR:
                    Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.board_general_title));
                    break;
                case Constants.BOARD_AUTOCLUB:
                    //isAutoFilter = true;
                    if(cbAutoFilter.size() > 0) {
                        clubTitle = createAutoClubTitle();
                        Objects.requireNonNull(getSupportActionBar()).setTitle(clubTitle);
                        menu.getItem(0).setVisible(true);
                    } else {
                        final String autoclub = getString(R.string.board_tab_title_autoclub);
                        Objects.requireNonNull(getSupportActionBar()).setTitle(autoclub);
                    }
                    animAutoFilter(true);
                    break;
                case Constants.BOARD_NOTIFICATION:
                    final String noti = getString(R.string.board_tab_title_notification);
                    Objects.requireNonNull(getSupportActionBar()).setTitle(noti);
                    binding.fabBoardWrite.setVisibility(View.INVISIBLE);
                    break;
            }

            category = position;
        }
    };

    // Implement AppBarLayout.OnOffsetChangedListener
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i){}


    // Floating Action Button click event handler
    // When clicking the fab to write a post, make BoardEditFragment null if it remains in the
    // framelayout. Then, check if the user has set a name. show the spanned message to guide the
    // the user to move SettingPreferenceActivity to create it.
    // The tab layout should be animated to decrease its height to 0 for purpose of adjusing the post
    // content area to the base line.
    // This event handler applies only to the floating action button.
    @Override
    public void onClick(View view) {
        if(view.getId() != R.id.fab_board_write) return;

        String userName = mSettings.getString(Constants.USER_NAME, null);
        if(TextUtils.isEmpty(userName)) {
            Snackbar snackbar = Snackbar.make(
                    binding.getRoot(), getString(R.string.board_msg_username), Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.board_msg_action_setting, v -> {
                Intent intent = new Intent(this, SettingActivity.class);
                intent.putExtra("caller", Constants.REQUEST_BOARD_SETTING_USERNAME);
                activityResultLauncher.launch(intent);
            }).show();

            return;
        }
        // TabHeight must be measured here b/c it will be increased to the full size or decreased
        // down to 0 each time the button clicks.

        // Create BoardWriteFragment with the user id attached. Remove any view in the framelayout
        // first and put the fragment into it.
        tabHeight = binding.tabBoard.getMeasuredHeight();
        if(binding.frameContents.getChildCount() > 0) binding.frameContents.removeView(binding.boardPager);
        if(editPostFragment != null) editPostFragment = null;

        writePostFragment = new BoardWriteFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putInt("tabPage", category);

        // Handle the toolbar menu as the write board comes in.
        if(category != Constants.BOARD_AUTOCLUB) {
            Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.board_title_write));
            animTabHeight(false); // false: tab height = 0;

        // Tapping the fab right when the viewpager holds the auto club page, animate to slide
        // the tablayout up to hide and slide the auto filter down to show sequentially. On the
        // other pages, animation is made to slide up not only the tablayout but also tne
        // nestedscrollview
        } else {

            //if(TextUtils.isEmpty(tvAutoFilterLabel.getText())) {
            // tvAutoFilterLabel should be null before calling createAutoFilterCheckbox().
            if(tvAutoFilterLabel == null) {
                Snackbar.make(binding.getRoot(), getString(R.string.board_autoclub_set), Snackbar.LENGTH_LONG).show();
                return;
            }

            tvAutoFilterLabel.setVisibility(View.GONE);
            cbGeneral.setVisibility(View.VISIBLE);

            //if(!isAutoFilter) animAutoFilter(isAutoFilter);
            animAutoFilter(true);
            menu.getItem(0).setVisible(false);
        }

        writePostFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                //.addToBackStack(null)
                .replace(binding.frameContents.getId(), writePostFragment)
                .commit();


        // Visibility control on menu and fab.
        binding.fabBoardWrite.setVisibility(View.INVISIBLE);
        menu.getItem(1).setVisible(true);
    }

    // Implement BoardReadDlgFragment.OnEditModeListener when the edit button presses.
    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onEditClicked(Bundle bundle) {
        log.i("onEditModeClicked");
        if(writePostFragment != null) writePostFragment = null;
        if(binding.frameContents.getChildAt(0) instanceof ViewPager2)
            binding.frameContents.removeView(binding.boardPager);

        editPostFragment = new BoardEditFragment();
        editPostFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().addToBackStack(null)
                .replace(binding.frameContents.getId(), editPostFragment)
                .commit();

        // Hide the emblem and set the club title if the current page is autoclub.
        if(category == Constants.BOARD_AUTOCLUB) Objects.requireNonNull(getSupportActionBar()).setTitle(clubTitle);
        else Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.board_title_edit));


        // Save the height of BoardTabLayout before its size becoms 0 to enable animTabHeight to be
        // workable as BoardPagerFragment comes in.(may replace getViewTreeObeserver() in onCreate().
        tabHeight = binding.tabBoard.getMeasuredHeight();
        animTabHeight(false);

        //if(menu.getItem(0).isVisible()) menu.getItem(0).setVisible(false);
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(true);
        binding.fabBoardWrite.setVisibility(View.GONE);
    }

    // ActivityResult callback.
    private void getSettingResultBack(ActivityResult result) {
        log.i("activity result:%s", result);
        if(result.getData() == null) return;

        switch(result.getResultCode()) {
            case Constants.REQUEST_BOARD_GALLERY:
                /*
                Uri uri = result.getData().getStringExtra("image");
                if(writePostFragment != null) writePostFragment.setUriFromImageChooser(uri);
                else if(editPostFragment != null) editPostFragment.setUriFromImageChooser(uri);
                 */
                break;

            case Constants.REQUEST_BOARD_SETTING_AUTOCLUB:
                jsonAutoFilter = result.getData().getStringExtra("autodata");
                log.i("json auto result: %s", jsonAutoFilter);
                // Create the autofilter checkboxes and set inital values to the checkboxes
                try { createAutoFilterCheckBox(this, jsonAutoFilter, binding.autofilter);}
                catch(NullPointerException e) { setNoAutoFilterText();}
                catch(JSONException e) {e.printStackTrace();}

                // Update the pagerAdapter
                pagerAdapter.setAutoFilterValues(cbAutoFilter);
                pagerAdapter.notifyItemChanged(Constants.BOARD_AUTOCLUB);
                binding.boardPager.setCurrentItem(Constants.BOARD_AUTOCLUB, true);

                clubTitle = createAutoClubTitle();
                if(getSupportActionBar() != null) getSupportActionBar().setTitle(clubTitle);
                addTabIconAndTitle(this, binding.tabBoard);
                //menu.getItem(0).setVisible(true);
                menu.getItem(0).getActionView().setVisibility(View.VISIBLE);
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
        if(getSupportActionBar() != null) getSupportActionBar().setTitle(clubTitle);
        // Referenced in BoardPagerFragment for purpose of requerying posts with new
        // conditions
        mListener.onCheckBoxValueChange(cbAutoFilter);

        // To enable the autoclub enabled when clicking the autofilter, the viewpager is set to
        // POST_NONE in getItemPosition() of BoardPagerAdapter, which destroys not only the autoclub
        // fragment but also the tab titles. Thus, recreate the title here.
        addTabIconAndTitle(this, binding.tabBoard);
        log.i("cbAutofilter checked: %s, %s", chkbox, isChecked);
        //if(!menu.getItem(1).isVisible()) menu.getItem(1).setVisible(true);
    }



    // Slide down the tab as the activity is created.
    private void animTabLayout() {
        //float y = (state)? getActionbarHeight() : 0;
        ObjectAnimator animTab = ObjectAnimator.ofFloat(binding.tabBoard, "y", getActionbarHeight());
        animTab.setDuration(500);
        animTab.start();
        // Upon completion of sliding down the tab, set the visibility of the viewpager and the
        // progressbar. If the activity gets started by clicking the buttons in the content title,
        // move to the page indicated by the cateogry, which is sent by Intent extra.
        animTab.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                binding.boardPager.setVisibility(View.VISIBLE);
                if(category != 0) binding.boardPager.setCurrentItem(category, false);
            }
        });
    }

    // When calling BoardWriteFragment, the tab layout is gone and the fragment comes in and vice versa.
    // state: true - full height false - 0 height.
    private void animTabHeight(boolean isShown) {
        isTabHeight = isShown;
        ValueAnimator anim = (isShown) ?
                ValueAnimator.ofInt(0, tabHeight) : ValueAnimator.ofInt(tabHeight, 0);
        anim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams params = binding.tabBoard.getLayoutParams();
            params.height = val;
            binding.tabBoard.setLayoutParams(params);
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
    private void animAutoFilter(boolean isAutoClub) {
        float y = (isAutoClub)? getActionbarHeight() : 0;
        ObjectAnimator slideAutoFilter = ObjectAnimator.ofFloat(binding.postScrollHorizontal, "y", y);
        slideAutoFilter.setDuration(500);
        slideAutoFilter.start();
        //isAutoFilter = !isAutoFilter;
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

        } else { ssb.append(chkboxList.get(0).getText());}

        ssb.append(String.format("%4s", getString(R.string.board_filter_club)));
        return ssb;
    }

    // In case that any auto filter that is initially saved as a json string is not set, show the
    // text which contains a clickable span to initiate SettingPrefActivity to set the auto filter.
    private void setNoAutoFilterText() {
        TextView tvMessage = new TextView(this);
        String msg = getString(R.string.board_autoclub_set);
        SpannableString ss = new SpannableString(msg);
        ClickableSpan clickableSpan = new ClickableSpan(){
            @Override
            public void onClick(@NonNull View textView) {
                log.i("autodata set clicked");
                int requestCode = Constants.REQUEST_BOARD_SETTING_AUTOCLUB;
                Intent intent = new Intent(BoardActivity.this, SettingActivity.class);
                intent.putExtra("caller", requestCode);
                activityResultLauncher.launch(intent);
            }
        };

        ss.setSpan(clickableSpan, 7, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvMessage.setText(ss);
        // Required to make ClickableSpan workable.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(10);
        tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
        binding.autofilter.addView(tvMessage, params);
    }

    /*
     * Dynamically create checkboxes based on the auto data saved in SharedPreferences as JSONString.
     *
     * If the auto data is empty or null, show the clickable spanned message to ask the user to set
     * auto data in SettingPreferenceActivity.
     *
     * If the the auto data is given, the checkbox list should differ according to which fragment
     * the framelayout contains. BoardWriteFragment adds an extra checkbox for whether a post may
     * read not only in the autoclub but also in the general board.
     *
     * Checked values in the checkbox list are passed to cbAutoFilter in order to be used as a query
     * condition on postings.
     *
     * @param context activity
     * @param json JSONString
     * @param v parent container
     * @throws JSONException may occur while converting JSONString to JSONArray
     */
    private void createAutoFilterCheckBox(Context context, String json, ViewGroup v) throws JSONException {
        // Remove the filter to switch the format b/w BoardPagerFragment and BoardWriteFragment.
        if(v.getChildCount() > 0) v.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(10);

        // TextUtils.isEmpty() does not properly work when it has JSONArray.optString(int) as params.
        // It is appropriate that JSONArray.isNull(int) be applied.
        JSONArray jsonAuto = new JSONArray(json);
        if(TextUtils.isEmpty(json) || jsonAuto.isNull(0)) throw new NullPointerException("no auto data");

        // Create the header and the general checkbox controlled by setting visibility which depends
        // on which fragment the frame contains; header turns visible in BoardPagerFragment and the
        // the checkbox indicating general post turns visible in BoardWriteFragment and vice versa.

        // Dynamically create either the autofilter label in BoardPagerFragment or the general checkbox
        // in BoardWritingFragment
        params.topMargin = 0;
        tvAutoFilterLabel = new TextView(context);
        tvAutoFilterLabel.setText(getString(R.string.board_filter_title));
        tvAutoFilterLabel.setTextColor(Color.WHITE);
        tvAutoFilterLabel.setTypeface(tvAutoFilterLabel.getTypeface(), Typeface.BOLD);
        v.addView(tvAutoFilterLabel, params);

        // Create the checkbox that indicates a general post shown not only in the general board
        // but also in the auto club.
        cbGeneral = new CheckBox(context);
        cbGeneral.setVisibility(View.GONE);
        cbGeneral.setText(getString(R.string.board_filter_chkbox_general));
        cbGeneral.setTextColor(Color.WHITE);
        v.addView(cbGeneral, params);
        cbGeneral.setOnCheckedChangeListener((chkbox, isChecked) -> isGeneral = isChecked);

        // Dynamically create the checkboxes. The automaker checkbox should be checked and disabled
        // as default values.
        isLocked = mSettings.getBoolean(Constants.AUTOCLUB_LOCK, false);
        for(int i = 0; i < jsonAuto.length(); i++) {
            CheckBox cb = new CheckBox(context);
            cb.setTag(i);
            cb.setTextColor(Color.WHITE);
            if(jsonAuto.optString(i).equals("null")) {
                log.i("autodata item:%s", jsonAuto.optString(i));
                switch(i) {
                    case 1: cb.setText(R.string.pref_auto_model);break;
                    case 2: cb.setText(R.string.pref_engine_type);break;
                    case 3: cb.setText(R.string.board_filter_year);break;
                }
                cb.setEnabled(false);

            } else {
                // The automaker is required to be checked as far as jsonAuto has been set not to be
                // null. Other autofilter values depends on whether it is the locked mode
                // which retrieves its value from SharedPreferences.
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
                cb.setOnCheckedChangeListener(this); //for setting the color and value
            }

            v.addView(cb, params);
            chkboxList.add(cb);
        }

        // Create the autofilter lock and handle the event when clicking the imageview.
        setAutoFilterLock(v, jsonAuto);
    }

    /*
     * Create ImageView and make action controls when clicking the view.
     * isLock is saved in SharedPreferences and if isLock is true, the checkboxes in the autofilter
     * turn disabled, retaining their values which have been respectively saved as well. When tapping
     * the imageview, isLock will be set to false, enabling the checkboxes to be ready to check
     * except one that has not been set.
     *
     * @param v parent viewgroup
     * @param jsonAuto CheckBox name in the autofilter
     */
    private void setAutoFilterLock(ViewGroup v, JSONArray jsonAuto) {
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

                } else if(!jsonAuto.optString(i).equals("null")) {
                    chkboxList.get(i).setEnabled(true);
                }
            }

            mSettings.edit().putBoolean(Constants.AUTOCLUB_LOCK, isLocked).apply();
        });

    }

    /*
     * Upon completion of uploading a post in BoardWriteFragment or BoardEditFragment when selecting
     * the upload menu, or upon cancellation of writing or editing a post when selecting the up Button,
     * remove the fragment out of the container, then regain the viewpager with the toolbar menu and
     * title reset. If the current page stays in the auto club, additional measures should be taken.
     * Aysnc issue may occur with FireStore. Thus, this method should be carefully invoked.
     */
    public void addViewPager() {
        // If any view exists in the framelayout, remove all views out of the layout and add the
        // viewpager
        if(binding.frameContents.getChildCount() > 0) {
            //binding.frameContents.removeView(binding.frameContents.getChildAt(0));
            binding.frameContents.removeAllViews();
        }
        // If the tabLayout height is 0,  put the height back to the default size.
        //if(!isTabHeight) animTabHeight(true);
        animTabHeight(true);

        binding.frameContents.addView(binding.boardPager);
        binding.fabBoardWrite.setVisibility(View.VISIBLE);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.board_general_title));

        // Animations should differ according to whether the current page is on the auto club.
        if(category == Constants.BOARD_AUTOCLUB) {
            // BUG: java.lang.NullPointerException: Attempt to invoke virtual method
            // 'void android.widget.TextView.setVisibility(int)' on a null object reference
            Objects.requireNonNull(tvAutoFilterLabel).setVisibility(View.VISIBLE);
            cbGeneral.setVisibility(View.GONE);

            getSupportActionBar().setTitle(createAutoClubTitle());
            if(!menu.getItem(0).isVisible()) menu.getItem(0).setVisible(true);
            if(menu.getItem(1).isVisible()) menu.getItem(1).setVisible(false);
        }

        //pagerAdapter.notifyDataSetChanged();
        log.i("board caregerory: %s", category);
        //pagerAdapter.notifyItemChanged(category);
        addTabIconAndTitle(this, binding.tabBoard);
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
                //startActivityForResult(galleryIntent, Constants.REQUEST_BOARD_GALLERY);
                //activityResultLauncher.launch(galleryIntent);
                break;

            case Constants.CAMERA: // Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Intent cameraChooser = Intent.createChooser(cameraIntent, "Choose camera");

                if(cameraIntent.resolveActivity(getPackageManager()) != null) {
                    //log.i("Camera Intent");
                    //startActivityForResult(cameraChooser, Constants.REQUEST_BOARD_CAMERA);
                    //activityResultLauncher.launch(cameraChooser);
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
        return binding.fabBoardWrite;
    }

    public SpannableStringBuilder getAutoClubTitle() {
        return clubTitle;
    }
    // Invoked in BoardPagerAdapter to hold visibility control to the progressbar when post logindg
    // complete and set it to gone in setFirstQuery().
    public ProgressBar getLoadingProgressBar() {
        return binding.progbarBoardLoading;
    }


}
