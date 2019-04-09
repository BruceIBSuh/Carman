package com.silverback.carman2;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.adapters.CarmanFragmentPagerAdapter;
import com.silverback.carman2.fragments.BoardFragment;
import com.silverback.carman2.fragments.FinishAppDialogFragment;
import com.silverback.carman2.fragments.GeneralFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.StationMapTask;

import java.io.File;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends BaseActivity implements
        FinishAppDialogFragment.NoticeDialogListener,
        ViewPager.OnPageChangeListener {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    // Objects
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Fragment generalFragment, boardFragment;
    private FrameLayout frameLayout;
    private StationMapTask mapInfoTask;
    private FinishAppDialogFragment alertDialog;

    // Fields
    private boolean isTabLayoutVisible = false;
    private float toolbarHeight;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        frameLayout = findViewById(R.id.frameLayout);
        tabLayout = findViewById(R.id.tabLayout);

        // Sets the toolbar used as ActionBar
        setSupportActionBar(toolbar);


        // Creates ViewPager programmatically and sets FragmentPagerAdapter to it, then interworks
        // with TabLayout
        viewPager = new ViewPager(this);
        viewPager.setId(View.generateViewId());
        FragmentPagerAdapter pagerAdapter = new CarmanFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(viewPager);

        // Custom method to set TabLayout title and icon, which MUST be invoked after
        // TabLayout.setupWithViewPager as far as TabLayout links with ViewPager.
        addTabIconAndTitle(tabLayout);

        // Get Defaults from BaseActivity and sets it bundled for passing to GeneralFragment
        String[] defaults = getDefaultParams();
        Bundle bundle = new Bundle();
        bundle.putStringArray("defaults", defaults);
        log.i("Default Params: %s, %s, %s", defaults[0], defaults[1], defaults[2]);

        // Instantiates Fragments which FrameLayout adds, replaces or removes a Fragment by selecting
        // a toolbar menu.
        generalFragment = new GeneralFragment();
        boardFragment = new BoardFragment();

        // Attaches GeneralFragment as a default display at first or returning from the fragments
        // picked up by Toolbar menus.
        generalFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, generalFragment, "generalFragment")
                .addToBackStack(null)
                .commit();

        // Calculates Toolbar height which is referred to as a baseline for TabLayout and ViewPager
        // to slide up and down.
        toolbarHeight = getActionbarHeight();
        log.i("toolbar height: %s",  toolbarHeight);

        // Permission Check
        checkPermissions();
    }

    // Callbacks invoked by Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.action_board:
                animSlideTabLayout();
                if(isTabLayoutVisible) getSupportFragmentManager().popBackStack();
                return true;

            case R.id.action_garage:
                // Custom method to animate the tab layout sliding up and down when clicking the buttons
                // on the toolbar(action bar). The TabLayout moves up and down by changing "Y" property
                // and the ViewPager does so by translating "Y".
                animSlideTabLayout();
                if(isTabLayoutVisible) getSupportFragmentManager().popBackStack();
                return true;

            case R.id.action_login:

                return true;

            case R.id.action_setting:
                startActivity(new Intent(MainActivity.this, GeneralSettingActivity.class));
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume(){
        super.onResume();
        String title = mSettings.getString(Constants.VEHICLE_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mapInfoTask != null) mapInfoTask = null;
    }

    // Callbacks invoked by ViewPager.OnPageChangeListener
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        log.d("ViewPager Listener_onPageScrolled");
    }
    @Override
    public void onPageSelected(int position) {
        log.d("ViewPager Listeenr_onPageSelected");
    }
    @Override
    public void onPageScrollStateChanged(int state) {
        log.d("ViewPager Listeenr_onPageScrollStateChanged");
    }


    @Override
    public void onBackPressed(){
        // Pop up the dialog to confirm to leave the app.
        alertDialog = new FinishAppDialogFragment();
        alertDialog.show(getSupportFragmentManager(), "FinishAppDialogFragment");
    }


    // App closing process, in which cache-clearing code be required.
    // FinishAppDialogFragment.NoticeDialogListener invokes
    // to handle how the dialog buttons act according to positive and negative.
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //boolean isDeleted = false;
        File cacheDir = getCacheDir();
        if(cacheDir != null && checkUpdateOilPrice()) {
            for (File file : cacheDir.listFiles()) file.delete();
        }

        this.finishAffinity();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}

    // Prgramatically, add titles and icons on the TabLayout, which must be invoked after
    // setupWithViewPager when it is linked to ViewPager.
    @SuppressWarnings("ConstantConditions")
    private void addTabIconAndTitle(TabLayout tabLayout) {

        final String[] titles = getResources().getStringArray(R.array.tap_title);
        final Drawable[] icons = new Drawable[] {
                getDrawable(R.drawable.ic_gas),
                getDrawable(R.drawable.ic_service),
                getDrawable(R.drawable.ic_stats)
                //getDrawable(R.drawable.ic_setting)
        };


        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(icons[i]);
            tabLayout.getTabAt(i).setText(titles[i]);
        }
    }

    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    private void animSlideTabLayout() {

        float tabEndValue = (!isTabLayoutVisible)? toolbarHeight : 0;

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frameLayout, "translationY", tabEndValue);
        slideTab.setDuration(500);
        slideViewPager.setDuration(500);

        if(!isTabLayoutVisible) {
            getSupportFragmentManager().beginTransaction().remove(generalFragment).commit();
            frameLayout.addView(viewPager);
        } else {
            frameLayout.removeView(viewPager);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frameLayout, generalFragment).addToBackStack(null).commit();
        }

        isTabLayoutVisible = !isTabLayoutVisible;

        slideTab.start();
        slideViewPager.start();

    }


    // Measures the size of an android attribute based on ?attr/actionBarSize
    private float getActionbarHeight() {
        TypedValue typedValue = new TypedValue();
        if(getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimension(typedValue.data, getResources().getDisplayMetrics());
        }
        return -1;
    }


}
