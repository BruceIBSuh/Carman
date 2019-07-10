package com.silverback.carman2;

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

    // The following methods are invoked by Toolbar working as Action Bar or Appbar.
    // When SettingServiceListFragment is launched,
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setting, menu);
        return true;
    }
    */

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        log.i("onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_setting, menu);
        menu.findItem(R.id.menu_add).setVisible(false);
        menu.findItem(R.id.menu_edit).setVisible(false);
        getMenuInflater().inflate(R.menu.menu_setting, menu);

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsItemSelected in SettingPreferenceActivity");
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // If you do not implement onPreferenceStartFragment(), a fallback implementation is used instead.
    // While this works in most cases, we strongly recommend implementing this method so you can fully
    // configure transitions between Fragment objects and update the title displayed in your
    // Activity toolbar, if applicable.
    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        log.d("onPreferenceStartFragment: title - %s", pref.getTitle());
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory()
                .instantiate(getClassLoader(), pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        getSupportActionBar().setTitle(pref.getTitle());
        log.i("PreferenceFragmentCompat: %s", pref.getKey());

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
                EditTextPreference pref = (EditTextPreference)settingFragment.findPreference(key);
                log.i("EditTextPref: %s", pref.getText());
                if(!TextUtils.isEmpty(pref.getText())) {
                    //pref.setSummary(pref.getText());
                    //mSettings.edit().putString(Constants.VEHICLE_NAME, pref.getText()).apply();
                }
                break;

            case Constants.ODOMETER:
                EditTextPreference mileage = (EditTextPreference)settingFragment.findPreference(key);
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
                //SwitchPreferenceCompat switchPref = (SwitchPreferenceCompat)settingFragment.findPreference(key);
                //log.i("SwitchPreferenceCompat: %s", switchPref.isChecked());
                //mSettings.edit().putBoolean("pref_location_autoupdate", switchPref.isChecked()).apply();
                break;
        }

    }

    // Callback by ThreadManager.startPriceTask when the task has the price info completed.
    public void onPriceTaskComplete() {
        //mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
    }
}