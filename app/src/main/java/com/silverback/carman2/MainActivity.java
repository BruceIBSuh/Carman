package com.silverback.carman2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.fragments.FinishAppDialogFragment;
import com.silverback.carman2.fragments.GeneralFragment;
import com.silverback.carman2.threads.GasPriceTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.ImageViewModel;
import com.silverback.carman2.viewmodels.OpinetViewModel;

import org.json.JSONArray;

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

    //private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    // Objects
    private CarmanDatabase mDB;
    private GasPriceTask gasPriceTask;
    private OpinetViewModel opinetModel;
    private GeneralFragment generalFragment;
    private ApplyImageResourceUtil imgResUtil;
    private ImageViewModel imgModel;
    //private ActionBarDrawerToggle drawerToggle;

    // Fields
    //private Drawable appbarIcon;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiation
        mDB = CarmanDatabase.getDatabaseInstance(this);
        imgResUtil = new ApplyImageResourceUtil(this);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);


        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        String title = mSettings.getString(Constants.USER_NAME, null);
        getSupportActionBar().setHomeButtonEnabled(false);
        if(title != null) getSupportActionBar().setTitle(title);

        /*
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name,R.string.app_name);
        */

        // Get the default value of fuel, searching radius, and listing order from BaseActivityR.drawable.logo_e1g
        // and set it to be bundled to pass it to GeneralFragment
        Bundle bundle = new Bundle();
        bundle.putStringArray("defaults", getDefaultParams());
        //bundle.putBoolean("notifyNetworkConnected", isNetworkConnected);
        generalFragment = new GeneralFragment();
        generalFragment.setArguments(bundle);
        // Attaches GeneralFragment as a default display at first or returning from the fragments
        // picked up by Toolbar menus.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_main, generalFragment, "general")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        // The activity gets started by the Geofence notification which launches GasManagerFragment
        // or ServiceManagerFragment at first and when clikcing the up button, the activity receives
        // the boolean extra that indicates how the app launches. This process has to call the price
        // task for displaying the price information.
        if(getIntent() != null && getIntent().getBooleanExtra("isGeofencing", false)) {
            mDB.favoriteModel().getFirstFavorite(Constants.GAS).observe(this, stnId -> {
                JSONArray json = getDistrictJSONArray();
                String distCode = (json != null) ?
                        json.optString(2) : getResources().getStringArray(R.array.default_district)[2];
                gasPriceTask = ThreadManager.startGasPriceTask(this, opinetModel, distCode, stnId);
            });
        }

        // MUST be located here b/c it has to be redrawn when startActivityForResult() is called.
        // Get the user image uri, if any, from SharedPreferences, then uses Glide to a drawable
        // fitting to the action bar, the result of which is notified using ImageViewModel.
        String userImage = mSettings.getString(Constants.USER_IMAGE, null);
        String imgUri = (TextUtils.isEmpty(userImage))? Constants.imgPath + "ic_user_blank_gray" : userImage;
        imgResUtil.applyGlideToDrawable(imgUri, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgModel);
        // In case the user image has changed in SettingPreferenceActivity, the uri of a new image
        // is sent in onActivityResult() as a result of startActivityForResult() which called
        // SettingPreferenceActivity. The img uri is processed to Drawable by Glide, the custom target
        // of which is passed via ImageViewModel and reset to the Icon when the activity resumes
        imgModel.getGlideDrawableTarget().observe(this, resource -> {
            if(getSupportActionBar() != null) getSupportActionBar().setIcon(resource);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if(gasPriceTask != null) gasPriceTask = null;
    }

    // startActivityForResult() has this callback invoked by getting an intent that contains new
    // values reset in SettingPreferenceActivity. The toolbar title should be replace with a new name
    // and PriceViewPager should be updated with a new district, both of which have been reset
    // in SettingPreferenceActivity.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode != Constants.REQUEST_MAIN_SETTING_OPTIONSITEM || resultCode != RESULT_OK)  return;

        boolean isDistrictReset = intent.getBooleanExtra("isDistrictReset", false);
        String userName = intent.getStringExtra("userName");
        String fuelCode = intent.getStringExtra("fuelCode");
        String radius = intent.getStringExtra("radius");
        String uriImage = intent.getStringExtra("userImage");

        // Must make the null check, not String.isEmpty() because the blank name should be included.
        if(userName != null) getSupportActionBar().setTitle(userName);

        if(uriImage != null)
            imgResUtil.applyGlideToDrawable(uriImage, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgModel);
        else imgModel.getGlideDrawableTarget().setValue(null);

        // Invalidate PricePagerView with new district and price data reset in SettingPreferenceActivity.
        generalFragment = ((GeneralFragment)getSupportFragmentManager().findFragmentByTag("general"));
        if(generalFragment != null) generalFragment.resetGeneralFragment(isDistrictReset, fuelCode, radius);
    }

    // DrawerLayout callbacks which should be applied at an appropritate time for update.
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
                int requestCode = Constants.REQUEST_MAIN_SETTING_OPTIONSITEM;
                settingIntent.putExtra("requestCode", requestCode);
                startActivityForResult(settingIntent, requestCode);
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

    // Implements FinsihAppDialogFragment.NoticeDialogListener, overriding onDialogPositiveClick()
    // and onDialogNegativeClick().
    // When clicking the confirm button, the app closes, clearing the cache files that contain
    // the gas prices as far as the update condition is met and garbage-collecting all the threads
    // in the threadpool. AT the same time, destroy the db instance which exists statically.
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

    // Referenced by child fragments.
    public SharedPreferences getSettings() {
        return mSettings;
    }
}
