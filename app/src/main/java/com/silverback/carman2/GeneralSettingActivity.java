package com.silverback.carman2;

import android.os.Bundle;
import com.silverback.carman2.fragments.GeneralSettingFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class GeneralSettingActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralSettingActivity.class);


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
        String districtCode = getDistrictCodeFromJSON();
        GeneralSettingFragment settingFragment = new GeneralSettingFragment();
        Bundle args = new Bundle();
        args.putString("districtCode", districtCode);
        settingFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, settingFragment)
                .commit();

    }


    // Invoked by PreferenceFragmentCompat.OnPrefrenceStartFragmentCallback to display a new
    // fragment when a linked preference is clicked
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        log.i("onPreferenceStartFragment");
        return false;
    }

    private String getDistrictCodeFromJSON() {

        try {
            JSONArray json = new JSONArray(mSettings.getString(Constants.DISTRICT, null));
            return json.getString(2);
        } catch(JSONException e) {
            log.i("JSONException: %s", e.getMessage());
        }

        return null;
    }
}