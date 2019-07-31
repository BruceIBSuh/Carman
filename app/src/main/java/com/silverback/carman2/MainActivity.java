package com.silverback.carman2;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.fragments.BillboardFragment;
import com.silverback.carman2.fragments.FinishAppDialogFragment;
import com.silverback.carman2.fragments.GeneralFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.StationInfoTask;

import java.io.File;
import java.util.List;

public class MainActivity extends BaseActivity implements
        FinishAppDialogFragment.NoticeDialogListener {
        //ViewPager.OnPageChangeListener {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    // Constants
    private static final int TAB_CARMAN = 1;
    private static final int TAB_BOARD = 2;

    // Objects
    private TabLayout tabLayout;
    private List<String> tabTitleList;
    private List<Drawable> tabIconList;
    //private ViewPager viewPager;
    private Fragment generalFragment, boardFragment;
    private FrameLayout frameLayout;
    private StationInfoTask mapInfoTask;
    private FinishAppDialogFragment alertDialog;

    // UIs
    private ProgressBar progressBar;
    // Fields
    private boolean isTabVisible = false;
    private int tabSelected;
    private float toolbarHeight;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_expense);
        frameLayout = findViewById(R.id.frameLayout);
        progressBar = findViewById(R.id.progbar);

        // Sets the toolbar used as ActionBar
        setSupportActionBar(toolbar);

        // Get Defaults from BaseActivity and sets it bundled for passing to GeneralFragment
        String[] defaults = getDefaultParams();
        Bundle bundle = new Bundle();
        bundle.putStringArray("defaults", defaults);
        log.i("Default Params: %s, %s, %s", defaults[0], defaults[1], defaults[2]);

        // Instantiates Fragments which FrameLayout adds, replaces or removes a Fragment by selecting
        // a toolbar menu.
        generalFragment = new GeneralFragment();
        boardFragment = new BillboardFragment();

        // Attaches GeneralFragment as a default display at first or returning from the fragments
        // picked up by Toolbar menus.
        generalFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, generalFragment, "general").addToBackStack(null).commit();

        // Permission Check
        checkPermissions();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume(){
        super.onResume();
        //getSupportFragmentManager().popBackStack();

        String title = mSettings.getString(Constants.VEHICLE_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);

        progressBar.setVisibility(View.GONE);
        frameLayout.setAlpha(1f);

    }

    @Override
    public void onPause() {
        super.onPause();
        if(mapInfoTask != null) mapInfoTask = null;
    }

    /*
     * The following 2 overriding methods are invoked by Toolbar working as Appbar or ActionBar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.action_carman:
                //progressBar.setVisibility(View.VISIBLE);
                //frameLayout.setAlpha(0.5f);
                startActivity(new Intent(MainActivity.this, ExpenseActivity.class));
                return true;

            case R.id.action_board:
                startActivity(new Intent(MainActivity.this, BillboardActivity.class));
                return true;

            case R.id.action_login:
                return true;

            case R.id.action_setting:
                startActivity(new Intent(MainActivity.this, SettingPreferenceActivity.class));
                return true;

            default:
                log.i("finish activity");
                finish();
        }

        return super.onOptionsItemSelected(item);
    }


    /*
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
    */


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

}
