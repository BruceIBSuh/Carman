package com.silverback.carman2;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.silverback.carman2.fragments.GeneralSettingFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class GeneralSettingActivity extends AppCompatActivity {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralSettingActivity.class);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_setting);

        Toolbar settingToolbar = findViewById(R.id.toolbar_setting);
        setSupportActionBar(settingToolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.frame_setting, new GeneralSettingFragment())
                .commit();

    }
}