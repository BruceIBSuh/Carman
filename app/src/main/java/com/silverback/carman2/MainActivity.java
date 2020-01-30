package com.silverback.carman2;

import android.content.Intent;
import android.content.SharedPreferences;
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
 * This activity is a container holding GeneralFragment which handles the district gas prices and
 * the latest gas and service expenditure, and near stations located in the default radius.
 * Additional fragment should be added to the activity at a later time to show a variety of contents
 * ahead of displaying GeneralFragment. Alternatively, the recyclerview positioned at the bottom for
 * showing near stations should be replaced with a view to display auto-related contents, making a
 * button to call DialogFragment or activity to display the near stations.
 */
public class MainActivity extends BaseActivity implements FinishAppDialogFragment.NoticeDialogListener {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);
    private final int REQ_SETTING = 1000;

    // Objects
    private GeneralFragment generalFragment;
    //private ActionBarDrawerToggle drawerToggle;

    // Fields
    private boolean isCreatedBySetting;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);//Sets the toolbar used as ActionBar
        String title = mSettings.getString(Constants.USER_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);

        /*
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name,R.string.app_name);
        */

        // Get the default value of fuel, searching radius, and listing order from BaseActivity
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

    // Receive an intent containing the user name and the Sido and Sigun district reset in
    // SettingPreferenceActivity. The toolbar title should be replace with the reset user name and
    // PriceViewPager should be updated with the reset district and price data retrieved and saved
    // in the storage.

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode != REQ_SETTING || resultCode != RESULT_OK)  return;

        boolean isDistrictReset = intent.getBooleanExtra("isDistrictChanged", false);
        String username = intent.getStringExtra("username");
        String fuelCode = intent.getStringExtra("fuelCode");
        log.i("Setting result: %s, %s, %s", isDistrictReset, username, fuelCode);

        // Reset the username in the toolbar if the name has been reset in SettingPreferenceActivity.
        if(username != null && getSupportActionBar() != null) getSupportActionBar().setTitle(username);

        // Invalidate PricePagerView with new district and price data reset in SettingPreferenceActivity.
        generalFragment = ((GeneralFragment)getSupportFragmentManager().findFragmentByTag("general"));
        if(generalFragment != null) {
            log.i("Reset requires the PriceViewPager refreshed");
            generalFragment.resetGeneralFragment(fuelCode, isDistrictReset);
        }

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
                // SettingPreferenceActivity if the values have changed to onActivityResult().
                Intent settingIntent = new Intent(this, SettingPreferenceActivity.class);
                startActivityForResult(settingIntent, REQ_SETTING);
                //startActivity(new Intent(this, SettingPreferenceActivity.class));
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
        if(cacheDir != null && checkPriceUpdate()) {
            for (File file : cacheDir.listFiles()) {
                if(!file.getName().equals(Constants.FILE_CACHED_STATION_PRICE))
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
