package com.silverback.carman2;

import android.content.SharedPreferences;
import android.os.Bundle;
import com.silverback.carman2.fragments.GeneralSettingFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.PriceTask;
import com.silverback.carman2.threads.ThreadManager;

import org.json.JSONArray;
import org.json.JSONException;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import static com.silverback.carman2.models.Constants.CODE;


public class GeneralSettingActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralSettingActivity.class);

    // Objects
    private GeneralSettingFragment settingFragment;
    private PriceTask priceTask;


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
        // display the custom DialogPreference.
        settingFragment = new GeneralSettingFragment();

        try {
            String district = new JSONArray(mSettings.getString(Constants.DISTRICT, null)).toString();
            Bundle args = new Bundle();
            args.putString(Constants.CODE, district);
            settingFragment.setArguments(args);

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

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

        if(key.equals(Constants.DISTRICT)) {
            try {
                JSONArray json = new JSONArray(mSettings.getString(Constants.DISTRICT, null));
                settingFragment.findPreference("pref_dialog_district")
                        .setSummary(String.format("%s %s", json.get(0), json.get(1)));

                priceTask = ThreadManager.startPriceTask(GeneralSettingActivity.this, json.get(2).toString());
                mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();

            } catch(JSONException e) {
                log.e("JSONException: %s", e.getMessage());
            }
        }
    }
}