package com.silverback.carman2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.fragments.FinishAppDialogFragment;
import com.silverback.carman2.fragments.GeneralFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;

import java.io.File;

/*
 * This activity is a container holding GeneralFragment which is composed of the district gas prices,
 * the latest gas and service expenditure, and near stations located in the default radius.
 * Additional fragment should be added to the activity at a later time to show a variety of contents
 * ahead of displaying GeneralFragment. Alternatively, the recyclerview positioned at the bottom for
 * near stations should be replaced with a view to display other auto-related contents, creating a
 * button to call the near station fragment.
 */
public class MainActivity extends BaseActivity implements FinishAppDialogFragment.NoticeDialogListener {
    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);
    private final int REQ_SETTING = 1000;

    // Objects
    private GeneralFragment generalFragment;
    //private ActionBarDrawerToggle drawerToggle;
    // Fields
    private Drawable appbarIcon;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);//Sets the toolbar used as ActionBar
        String title = mSettings.getString(Constants.USER_NAME, null);
        getSupportActionBar().setHomeButtonEnabled(false);
        if(title != null) getSupportActionBar().setTitle(title);

        // Get the user image uri, if any, from SharedPreferences
        String userImage = mSettings.getString(Constants.USER_IMAGE, null);
        appbarIcon = setUserImageToIcon(userImage); //REFACTOR USING GLIDE!!!
        getSupportActionBar().setIcon(appbarIcon);

        /*
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name,R.string.app_name);
        */

        // Get the default value of fuel, searching radius, and listing order from BaseActivityR.drawable.logo_e1g
        // and set it to be bundled to pass it to GeneralFragment
        Bundle bundle = new Bundle();
        bundle.putStringArray("defaults", getDefaultParams());
        bundle.putBoolean("notifyNetworkConnected", isNetworkConnected);
        generalFragment = new GeneralFragment();
        generalFragment.setArguments(bundle);
        // Attaches GeneralFragment as a default display at first or returning from the fragments
        // picked up by Toolbar menus.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_main, generalFragment, "general")
                .addToBackStack(null)
                .commit();

        // Permission Check which is initiated by the parent activty(BaseActivity), so it is of no
        // use to initiate the permission check again here.
        //checkPermissions();
    }

    // startActivityForResult() has this callback invoked by getting an intent that contains new
    // values reset in SettingPreferenceActivity. The toolbar title should be replace with a new name
    // and PriceViewPager should be updated with a new district, both of which have been reset
    // in SettingPreferenceActivity.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode != REQ_SETTING || resultCode != RESULT_OK)  return;

        boolean isDistrictReset = intent.getBooleanExtra("isDistrictReset", false);
        String userName = intent.getStringExtra("userName");
        String fuelCode = intent.getStringExtra("fuelCode");
        String radius = intent.getStringExtra("radius");
        String uriImage = intent.getStringExtra("userImage");

        // Must make the null check, not String.isEmpty() because the blank name should be included.
        if(userName != null) getSupportActionBar().setTitle(userName);
        if(uriImage != null) {
            Drawable newIcon = setUserImageToIcon(uriImage);
            getSupportActionBar().setIcon(newIcon);
        } else getSupportActionBar().setIcon(null);

        // Invalidate PricePagerView with new district and price data reset in SettingPreferenceActivity.
        generalFragment = ((GeneralFragment)getSupportFragmentManager().findFragmentByTag("general"));
        if(generalFragment != null) generalFragment.resetGeneralFragment(isDistrictReset, fuelCode, radius);


    }
    /*
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
    */


    //The following callback methods are invoked by Toolbar working as Appbar or ActionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.action_garage:
                startActivity(new Intent(MainActivity.this, ExpenseActivity.class));
                return true;

            case R.id.action_board:
                startActivity(new Intent(this, BoardActivity.class));
                return true;

            case R.id.action_login:
                return true;

            case R.id.action_setting:
                // Apply startActivityForresult() to take the price data and the username back from
                // SettingPreferenceActivity to onActivityResult() if the values have changed.
                Intent settingIntent = new Intent(this, SettingPreferenceActivity.class);
                startActivityForResult(settingIntent, REQ_SETTING);
                return true;

            default:
                finish();
                break;
        }

        return false;
    }

    @Override
    public void onBackPressed(){
        // Pop up the dialog to confirm to leave the app.
        FinishAppDialogFragment alertDialog = new FinishAppDialogFragment();
        alertDialog.show(getSupportFragmentManager(), "FinishAppDialogFragment");
    }


    // App closing process, in which cache-clearing code be required.
    // FinishAppDialogFragment.NoticeDialogListener invokes
    // to handle how the dialog buttons act according to positive and negative.
    // The station price file named FILE_CACHED_STATION_PRICE is excluded to delete because
    // it should retain the price to calculate the difference in the current and previous price.
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        File cacheDir = getCacheDir();
        // CacheDir contains not only the price data of the average, sido, sigun but also the near
        // stations. On leaving the app, clear the cache directory as far as the checkPriceUpdate
        // is satisfied.
        if(cacheDir != null && checkPriceUpdate()) {
            for (File file : cacheDir.listFiles()) {
                //if(!file.getName().equals(Constants.FILE_CACHED_STATION_PRICE))
                file.delete();
            }
        }

        ThreadManager.cancelAllThreads();
        if(CarmanDatabase.getDatabaseInstance(this) != null) CarmanDatabase.destroyInstance();
        finishAffinity();
    }
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}


    public SharedPreferences getSettings() {
        return mSettings;
    }




}
