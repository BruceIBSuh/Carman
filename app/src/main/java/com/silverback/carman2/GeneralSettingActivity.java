package com.silverback.carman2;

import android.os.Bundle;
import com.silverback.carman2.fragments.GeneralSettingFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
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

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, new GeneralSettingFragment())
                .commit();

    }


    // Invoked by PreferenceFragmentCompat.OnPrefrenceStartFragmentCallback to display a new
    // fragment when a linked preference is clicked
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        return false;
    }
}