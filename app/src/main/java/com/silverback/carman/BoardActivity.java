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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.PropertyName;
import com.silverback.carman.adapters.BoardPagerAdapter;
import com.silverback.carman.databinding.ActivityBoardBinding;
import com.silverback.carman.fragments.BoardEditFragment;
import com.silverback.carman.fragments.BoardWriteDlgFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
 * toolbar as long as the fragment to read a post(BoardReadFragment) pops up and the post is
 * owned by the user.
 *
 * Communications b/w the fragments are mostly made with the livedata defined in FramentSharedModel.
 * Some cases use interfaces, though.
 *
 * OnAutoFilterCheckBoxListener passes any change of the checkbox values to BoardPagerFragment for
 * dynamically querying posts based on it.  OnEditModeListener defined in BoardReadFragment
 * notifies that the user chooses the edit button to open BoardEditFragment.
 *
 * The toolbar menu should be basically handled in the parent activity but may be controlled by
 * each fragment. Thus, the return boolean value in OnOptionsItemSelected() depends on whether the
 * menu proceed(false) or consume(true).
 */

public class BoardActivity extends BaseActivity implements
        View.OnClickListener,
        CheckBox.OnCheckedChangeListener,
        AppBarLayout.OnOffsetChangedListener {
        //BoardReadFragment.OnEditModeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardActivity.class);

    public static final int PAGINATION = 10;
    public static final int PAGING_COMMENT = 3;
    public static final int PAGING_REPLY = 3;
    public static final int RECENT = 0;
    public static final int POPULAR = 1;
    public static final int AUTOCLUB = 2;
    public static final int NOTIFICATION = 3;

    // Objects
    private ActivityBoardBinding binding;
    private BoardPagerAdapter pagerAdapter;
    private BoardWriteDlgFragment writePostFragment;
    private MenuItem menuItem;
    private TextView tvMessage;

    // Fields
    private List<CheckBox> chkboxList;
    private ArrayList<String> cbAutoFilter;//having checkbox values for working as autofilter.
    //private boolean isGeneral; //check if a post should be uploaded to the general or just auto.
    private String jsonAutoFilter; //auto data saved in SharedPreferences as JSON String.
    private SpannableStringBuilder clubTitle;
    //private int tabHeight;
    //private int tabPage;
    //private boolean isAutoFilter, isTabHeight, isLocked;
    private boolean isLocked;
    private int category;
    private Uri photoUri;

    // Getting preference values from SettingActivity
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::getSettingResultBack);
    // Getting Uri from the image media
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::getAttachedImageUri);
    // Getting Uri from Camera
    private final ActivityResultLauncher<Uri> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), this::getCameraImage);


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBoardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.boardToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.board_general_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Change the progressbar color using the PorterDuff filter
        binding.progbarBoardLoading.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this, android.R.color.holo_blue_dark),
                android.graphics.PorterDuff.Mode.SRC_IN);


        // chkboxList is created by whether the autodata is set. cbAutoFilter is created
        // by wheteher each checkbox item is checked.
        chkboxList = new ArrayList<>();
        cbAutoFilter = new ArrayList<>();
        category = RECENT;

        // Create the autofilter checkbox if the user's auto data is set. If null, it catches the
        // exception that calls setNoAutofilterText().
        jsonAutoFilter = mSettings.getString(Constants.AUTO_DATA, null);
        try { createAutoFilterCheckBox(this, jsonAutoFilter, binding.autofilter);}
        catch(NullPointerException e) {setNoAutoFilterText();}
        catch(JSONException e) {e.printStackTrace();}

        // ViewPager2
        pagerAdapter = new BoardPagerAdapter(getSupportFragmentManager(), getLifecycle(), cbAutoFilter);
        //pagerAdapter.setAutoFilterValues(cbAutoFilter);
        binding.boardPager.setAdapter(pagerAdapter);
        binding.boardPager.setVisibility(View.GONE);//show progressbar unitl the query completes.

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
        /*
        getSupportFragmentManager().addFragmentOnAttachListener((fm, fragment) -> {
            //if(fragment instanceof BoardReadFragment) {
                log.i("BoardReadFragment added");
                //BoardReadFragment readFragment = (BoardReadFragment)fragment;
                //readFragment.setEditModeListener(this);
            //}
        });
         */


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
     * are presented in the appbar. When an event occurs and you want to make a menu update,
     * you must call invalidateOptionsMenu() to request that the system call onPrepareOptionsMenu().
     *
     * What's difference b/w return true and super.onCreateOptionsMenu(menu) is, if true, no menu
     * items are able to be set in the child.
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_board, menu);
        menuItem = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    public void getPermissionResult(Boolean isPermitted) {
        log.i("permission result: %s", isPermitted);
    }

    // Implement the abstract class of ViewPager2.OnPageChangeCallback, which has replaced the previous
    // interface type.
    private final ViewPager2.OnPageChangeCallback pagerCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            category = position;
            binding.fabBoardWrite.setVisibility(View.VISIBLE);
            switch(position) {
                case RECENT | POPULAR:
                    animAutoFilter(false);
                    final String general = getString(R.string.board_general_title);
                    Objects.requireNonNull(getSupportActionBar()).setTitle(general);
                    break;
                case AUTOCLUB:
                    animAutoFilter(true);
                    if(cbAutoFilter.size() > 0) {
                        clubTitle = createAutoClubTitle();
                        Objects.requireNonNull(getSupportActionBar()).setTitle(clubTitle);
                        menuItem.setVisible(true);
                    } else {
                        final String autoclub = getString(R.string.board_tab_title_autoclub);
                        Objects.requireNonNull(getSupportActionBar()).setTitle(autoclub);
                    }
                    break;

                case NOTIFICATION:
                    animAutoFilter(false);
                    binding.fabBoardWrite.setVisibility(View.INVISIBLE);
                    final String noti = getString(R.string.board_tab_title_notification);
                    Objects.requireNonNull(getSupportActionBar()).setTitle(noti);
                    break;
            }
        }
    };

    // Implement AppBarLayout.OnOffsetChangedListener
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i){}

    // Floating Action Button event handler to write a post. User name is required.
    @Override
    public void onClick(View view) {
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
        //tabHeight = binding.tabBoard.getMeasuredHeight();

        // With the user name set, call the dialogfragmt for writing a post.
        writePostFragment = new BoardWriteDlgFragment();
        log.i("user profile: %s, %s", userId, userName);
        Bundle args = new Bundle();
        args.putString("userId", userId); // userId defined in BaseActivity
        args.putString("userName", userName);
        args.putInt("page", category);
        if(category == AUTOCLUB) args.putString("autofilter", jsonAutoFilter);

        writePostFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, writePostFragment)
                .addToBackStack(null)
                .commit();
    }

    // Inplement CheckBox.OnCheckedChangedListener to notify that a checkbox chagnes its value.
    // CheckBox values will be used for conditions for querying posts. At the same time, the automodel
    // checkbox state will manage to set the toolbar title.
    @Override
    public void onCheckedChanged(CompoundButton chkbox, boolean isChecked) {
        final int index = (int)chkbox.getTag();
        log.i("checkbox index:%s", index);
        if(isChecked) {
            cbAutoFilter.add(chkbox.getText().toString());
            //chkboxList.add(index, (CheckBox)chkbox);
            /*
            if(index == 1) cbAutoFilter.add(index, chkbox.getText().toString());
            else cbAutoFilter.add(chkbox.getText().toString());
             */
        } else {
            cbAutoFilter.remove(chkbox.getText().toString());
            //chkboxList.remove(index);
        }


        //for(String filter : cbAutoFilter) log.i("filter : %s", filter);
        // As far as the automodel checkbox value changes, the toolbar title will be reset using
        // creteAutoClubTitle().

        clubTitle = createAutoClubTitle();
        log.i("club title change: %s", clubTitle);
        if(getSupportActionBar() != null) getSupportActionBar().setTitle(clubTitle);

        // Referenced in BoardPagerFragment for purpose of requerying posts with new
        // conditions
        //mListener.onCheckBoxValueChange(cbAutoFilter);
        pagerAdapter.getPagerFragment().setCheckBoxValueChange(cbAutoFilter);


        // To enable the autoclub enabled when clicking the autofilter, the viewpager is set to
        // POST_NONE in getItemPosition() of BoardPagerAdapter, which destroys not only the autoclub
        // fragment but also the tab titles. Thus, recreate the title here.
        addTabIconAndTitle(this, binding.tabBoard);
        //if(!menu.getItem(1).isVisible()) menu.getItem(1).setVisible(true);
    }

    protected void addTabIconAndTitle(Context context, TabLayout tabLayout) {
        List<String> tabTitleList = null;
        List<Drawable> tabIconList = null;

        tabTitleList = Arrays.asList(getResources().getStringArray(R.array.board_tab_title));

        /*
        if(context instanceof ExpenseActivity) {
            tabTitleList = Arrays.asList(getResources().getStringArray(R.array.tab_carman_title));
            Drawable[] icons = {
                    AppCompatResources.getDrawable(this, R.drawable.ic_gas),
                    AppCompatResources.getDrawable(this, R.drawable.ic_service),
                    AppCompatResources.getDrawable(this, R.drawable.ic_stats)};
            tabIconList = Arrays.asList(icons);

        } else if(context instanceof BoardActivity) {
            tabTitleList = Arrays.asList(getResources().getStringArray(R.array.board_tab_title));
        }

         */

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            Objects.requireNonNull(tabLayout.getTabAt(i)).setText(tabTitleList.get(i));
            //Objects.requireNonNull(tabLayout.getTabAt(i)).setIcon(tabIconList.get(i));
        }
    }

    // Slide down the tab as the activity is created.
    public void animTabLayout() {
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
    /*
    private void animTabHeight(boolean isShown) {
        //isTabHeight = isShown;
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
     */

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
        log.i("chkboxList: %s", chkboxList.size());
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if(chkboxList.get(1) != null && chkboxList.get(1).isChecked()) {
            ssb.append(chkboxList.get(1).getText()).append(" ").append(chkboxList.get(0).getText());
            for(int i = 2; i < chkboxList.size(); i++) {
                if(chkboxList.get(i).isChecked()) ssb.append(" ").append(chkboxList.get(i).getText());
            }

            int start = cbAutoFilter.get(1).length() + 1;
            int end = ssb.length();
            ssb.setSpan(new RelativeSizeSpan(0.6f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else { ssb.append(chkboxList.get(0).getText());}

        ssb.append(String.format("%4s", getString(R.string.board_filter_club)));
        return ssb;
    }

    // In case that any auto filter that is initially saved as a json string is not set, show the
    // text which contains a clickable span to initiate SettingPrefActivity to set the auto filter.
    private void setNoAutoFilterText() {
        tvMessage = new TextView(this);
        SpannableString ss = new SpannableString(getString(R.string.board_autoclub_set));
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

        ss.setSpan(clickableSpan, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(Color.RED), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvMessage.setText(ss);
        // Required to make ClickableSpan workable.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(24);
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
        /*
        if(v.getChildCount() > 2) {
            v.removeAllViews();
            chkboxList.clear();
        }
         */

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(10);

        // TextUtils.isEmpty() does not properly work when it has JSONArray.optString(int) as params.
        // It is appropriate that JSONArray.isNull(int) be applied.
        JSONArray jsonAuto = new JSONArray(json);
        if(TextUtils.isEmpty(json) || jsonAuto.isNull(0)) throw new NullPointerException("no auto data");

        // Dynamically create the checkboxes. The automaker checkbox should be checked and disabled
        // as default values.
        isLocked = mSettings.getBoolean(Constants.AUTOCLUB_LOCK, false);
        jsonAuto.remove(2);//Exclude the auto type.
        for(int i = 0; i < jsonAuto.length(); i++) {
            CheckBox cb = new CheckBox(context);
            cb.setTag(i);
            cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            cb.setTextColor(Color.WHITE);
            chkboxList.add(i, cb);

            if(jsonAuto.optString(i).equals("null")) {
                log.i("autodata item:%s", jsonAuto.optString(i));
                switch(i) {
                    case 1: cb.setText(R.string.pref_auto_model);break;
                    case 2: cb.setText(R.string.pref_engine_type);break;
                    case 3: cb.setText(R.string.board_filter_year);break;
                }
                cb.setEnabled(false);

            } else {
                cb.setText(jsonAuto.optString(i));
                if(i == 0) {
                    cb.setChecked(true);
                    cb.setEnabled(false);
                } else {
                    final String key = Constants.AUTOFILTER + i;
                    boolean b = mSettings.getBoolean(key, false);
                    cb.setChecked(b);
                    if(isLocked) cb.setEnabled(false);
                }
                // Add the checkbox value to the list if it is checked.
                if(cb.isChecked()) cbAutoFilter.add(cb.getText().toString());
                cb.setOnCheckedChangeListener(this);
            }

            v.addView(cb, params);
        }

        //Event hander for the lock button
        binding.imgbtnLock.setOnClickListener(imgview -> {
            isLocked = !isLocked;
            int res = (isLocked)? R.drawable.ic_autofilter_lock : R.drawable.ic_autofilter_unlock;
            binding.imgbtnLock.setImageResource(res);

            //Persist each checkbox value in the setting
            mSettings.edit().putBoolean(Constants.AUTOCLUB_LOCK, isLocked).apply();
            for(int i = 1; i < chkboxList.size(); i++) {
                if(isLocked) {
                    chkboxList.get(i).setEnabled(false);
                    String key = Constants.AUTOFILTER + i;
                    mSettings.edit().putBoolean(key, chkboxList.get(i).isChecked()).apply();
                } else if(!jsonAuto.optString(i).equals("null")) {
                    chkboxList.get(i).setEnabled(true);
                }
            }
        });
    }

    // Referenced either in BoardWriteFragmnet or in BoardEditFragment and notified of which media
    // (camera or gallery) to select in ImageChooserFragment. According to the selected media,
    // startActivityForResult() defined in the parent activity is invoked and the result is notified
    // to the activity and it is, in turn, sent back here by calling
    public void selectImageMedia(int media, View rootView) {
        switch(media) {
            case Constants.GALLERY:
                mGetContent.launch("image/*");
                break;
            case Constants.CAMERA:
                String rationale = "permission required to use camera";
                checkRuntimePermission(rootView, Manifest.permission.CAMERA, rationale, () -> {
                    File tmpFile = new File(getCacheDir(), new SimpleDateFormat(
                            "yyyyMMdd_HHmmss", Locale.US ).format(new Date( )) + ".jpg" );
                    photoUri = FileProvider.getUriForFile(this, Constants.FILE_IMAGES, tmpFile);
                    mTakePicture.launch(photoUri);
                });
                break;

            default: break;
        }
    }

    // ActivityResultCallback for ActivityResultContracts.GetContent()
    public void getAttachedImageUri(Uri uri) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);
        if(fragment instanceof BoardWriteDlgFragment) writePostFragment.addImageThumbnail(uri);
        else if(fragment instanceof BoardEditFragment) ((BoardEditFragment)fragment).addImageThumbnail(uri);
    }

    // ActivityResultCalback for ActivityResultContract.TakePicture();
    private void getCameraImage(boolean isTaken) {
        if(isTaken) writePostFragment.addImageThumbnail(photoUri);
    }

    // ActivityResultCallback for ActivityResultContracts.StartActivityForResult()
    private void getSettingResultBack(ActivityResult result) {
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
                binding.autofilter.removeView(tvMessage);
                jsonAutoFilter = result.getData().getStringExtra("autodata");
                // Create the autofilter checkboxes and set inital values to the checkboxes
                try { createAutoFilterCheckBox(this, jsonAutoFilter, binding.autofilter);}
                catch(NullPointerException e) { setNoAutoFilterText();}
                catch(JSONException e) {e.printStackTrace();}

                // Update the pagerAdapter
                //pagerAdapter.setAutoFilterValues(cbAutoFilter);
                pagerAdapter.notifyItemChanged(AUTOCLUB);
                binding.boardPager.setCurrentItem(AUTOCLUB, true);

                clubTitle = createAutoClubTitle();
                if(getSupportActionBar() != null) getSupportActionBar().setTitle(clubTitle);
                addTabIconAndTitle(this, binding.tabBoard);
                menuItem.getActionView().setVisibility(View.VISIBLE);
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

    // Refactor required!!:
    // Custom class to typecast Firestore array field of post_images to ArrayList<String> used
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
}
