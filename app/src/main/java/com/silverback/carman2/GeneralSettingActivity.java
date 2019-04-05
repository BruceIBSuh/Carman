package com.silverback.carman2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import com.silverback.carman2.fragments.GeneralSettingFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.PriceTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.views.SpinnerDialogPreference;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;


public class GeneralSettingActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralSettingActivity.class);

    // Objects
    private GeneralSettingFragment settingFragment;
    private PriceTask priceTask;
    private String districtCode;


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_setting);

        Toolbar settingToolbar = findViewById(R.id.toolbar_setting);
        setSupportActionBar(settingToolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        // Passes the District Code to GeneralSettingFragment(PreferenceFragmentCompat) to
        // set the summary of SinnerDialogPreference
        settingFragment = new GeneralSettingFragment();

        districtCode = convJSONArrayToList().get(2);
        String vehicleName = mSettings.getString(Constants.VEHICLE_NAME, null);


        Bundle args = new Bundle();
        args.putStringArray("district", convJSONArrayToList().toArray(new String[3]));
        args.putString("name", vehicleName);

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


    // Invoked by PreferenceFragmentCompat.OnPrefrenceStartFragmentCallback to display a new
    // fragment when a linked preference is clicked
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        log.i("onPreferenceStartFragment");
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch(key) {
            case Constants.DISTRICT:
                districtCode = convJSONArrayToList().get(2);
                priceTask = ThreadManager.startPriceTask(this, districtCode);
                mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
                break;
            case Constants.VEHICLE_NAME:
                EditTextPreference pref = (EditTextPreference)settingFragment.findPreference(key);
                log.i("EditTextPref: %s", pref.getText());
                if(!TextUtils.isEmpty(pref.getText())) {
                    pref.setSummary(pref.getText());
                    mSettings.edit().putString(Constants.VEHICLE_NAME, pref.getText()).apply();
                }
                break;
        }

    }

    // Callback by ThreadManager.startPriceTask when the task has the price info completed.
    public void onPriceTaskComplete() {
        //mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
    }
}