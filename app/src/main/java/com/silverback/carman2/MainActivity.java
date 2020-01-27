package com.silverback.carman2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.silverback.carman2.adapters.PricePagerAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.fragments.FinishAppDialogFragment;
import com.silverback.carman2.fragments.GeneralFragment;
import com.silverback.carman2.fragments.PricePagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;

import java.io.File;

/*
 * This activity is a container holding GeneralFragment which displays the gas prices and the recent
 * expenditure of gas and service, and stations in the default radius based on the current location.
 * It should be replaced with alternative fragment that shows a content instead of the near stations.
 */
public class MainActivity extends BaseActivity implements FinishAppDialogFragment.NoticeDialogListener {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);
    private final int REQ_SETTING = 1;

    // Objects
    private Fragment generalFragment;
    private CarmanDatabase mDB;
    private OpinetViewModel opinetViewModel;
    //private ActionBarDrawerToggle drawerToggle;

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isDistReset = data.getBooleanExtra("resetDistrict", false);
        String username = data.getStringExtra("resetName");
        log.i("Setting result: %s, %s", isDistReset, username);

        // Reset the username in the toolbar if the name has been reset in SettingPreferenceActivity.
        if(username != null && getSupportActionBar() != null) getSupportActionBar().setTitle(username);

        // Invalidate PricePagerView with new district and price data reset in SettingPreferenceActivity.
        GeneralFragment fragment = ((GeneralFragment)getSupportFragmentManager().findFragmentByTag("general"));
        if(fragment != null && isDistReset) {
            log.i("Reset requires the PriceViewPager refreshed");
            fragment.resetPricePager();
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

            case R.id.action_garage:
                startActivity(new Intent(MainActivity.this, ExpenseActivity.class));
                return true;

            case R.id.action_board:
                startActivity(new Intent(this, BoardActivity.class));
                return true;

            case R.id.action_login:
                return true;

            case R.id.action_setting:
                //startActivity(new Intent(MainActivity.this, SettingPreferenceActivity.class));
                Intent settingIntent = new Intent(this, SettingPreferenceActivity.class);
                startActivityForResult(settingIntent, REQ_SETTING);
                return true;

            default:
                log.i("finish activity");
                finish();
        }

        return super.onOptionsItemSelected(item);
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
