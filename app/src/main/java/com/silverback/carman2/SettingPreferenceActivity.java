package com.silverback.carman2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.silverback.carman2.fragments.SettingPreferenceFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.PriceTask;
import com.silverback.carman2.threads.ThreadManager;

import java.text.DecimalFormat;
import java.util.List;


public class SettingPreferenceActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingPreferenceActivity.class);

    // Objects
    private MenuItem menuEdit, menuAdd;
    private PreferenceFragmentCompat caller;
    private SettingPreferenceFragment settingFragment;
    private PriceTask priceTask;
    private String distCode;
    private DecimalFormat df;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_setting);

        Toolbar settingToolbar = findViewById(R.id.toolbar_setting);
        setSupportActionBar(settingToolbar);
        // Get a support ActionBar corresponding to this toolbar
        //ActionBar ab = getSupportActionBar();
        // Enable the Up button which enables it as an action button such that when the user presses
        // it, the parent activity receives a call to onOptionsItemSelected().
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // DecimalFormat singleton instance from BaseActivity
        df = getDecimalFormatInstance();


        // Passes District Code(Sigun Code) and vehicle nickname to SettingPreferenceFragment for
        // setting the default spinner values in SpinnerDialogPrefernce and showing the summary
        // of the vehicle name respectively.
        List<String> district = convJSONArrayToList();
        if(district == null) distCode = "0101";
        else distCode = district.get(2);
        String vehicleName = mSettings.getString(Constants.VEHICLE_NAME, null);

        Bundle args = new Bundle();
        args.putStringArray("district", convJSONArrayToList().toArray(new String[3]));
        args.putString("name", vehicleName);
        //args.putString(Constants.ODOMETER, mileage);
        settingFragment = new SettingPreferenceFragment();
        settingFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, settingFragment)
                .commit();
    }


    @Override
    public void onResume(){
        super.onResume();
        mSettings.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettings.unregisterOnSharedPreferenceChangeListener(this);
        if(priceTask != null) priceTask = null;
    }



    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch(item.getItemId()) {
            case android.R.id.home:
                log.i("onOptionsItemSelected in SettingPreferenceActivity");
                Intent mainIntent = new Intent(this, MainActivity.class);
                startActivity(mainIntent);
                finish();

                //onBackPressed();
                return true;

            case R.id.menu_add:
                log.i("onOptionsItemSelected: menu_add");

                return true;
            case R.id.menu_edit:
                break;

        }

        return super.onOptionsItemSelected(item);
    }
    */


    /*
     * Invoked when a Preference with an associated Fragment is tabbed.
     * If you do not implement on PreferenceStartFragment(), a fallback implementation is used instead.
     * While this works i most cases, we strongly recommend implementing this method so you can fully
     * configure transitions b/w Fragment objects and update the title in the toolbar, if applicable.
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {

        log.i("Preference tabbed: %s", pref);
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());

        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        getSupportActionBar().setTitle(pref.getTitle());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch(key) {
            case Constants.VEHICLE_NAME:
                EditTextPreference pref = settingFragment.findPreference(key);
                log.i("EditTextPref: %s", pref.getText());
                if(!TextUtils.isEmpty(pref.getText())) {
                    //pref.setSummary(pref.getText());
                    //mSettings.edit().putString(Constants.VEHICLE_NAME, pref.getText()).apply();
                }
                break;

            case Constants.ODOMETER:
                EditTextPreference mileage = settingFragment.findPreference(key);
                log.i("EditTextPref: %s", mileage.getText());
                if(!TextUtils.isEmpty(mileage.getText())) {
                    //mileage.setSummary(mileage.getText() + "km");
                    //mSettings.edit().putString(Constants.ODOMETER, mileage.getText()).apply();
                }

                break;

            case Constants.DISTRICT:
                log.i("District changed");
                distCode = convJSONArrayToList().get(2);
                priceTask = ThreadManager.startPriceTask(this, distCode);
                mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();

                break;

            case "pref_location_autoupdate":
                break;
        }

    }

    // Callback by ThreadManager.startPriceTask when the task has the price info completed.
    public void onPriceTaskComplete() {
        //mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
    }

    // Custom method that fragments herein may refer to SharedPreferences inherited from BaseActivity.
    public SharedPreferences getSettings() {
        return mSettings;
    }
}