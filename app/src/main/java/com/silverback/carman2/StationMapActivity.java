package com.silverback.carman2;

import android.os.Bundle;
import android.widget.TextView;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class StationMapActivity extends AppCompatActivity {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationMapActivity.class);

    // Objects

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_map);

        // Set ToolBar as ActionBar and attach Home Button and title on it.
        Toolbar mapToolbar = findViewById(R.id.tb_map);
        setSupportActionBar(mapToolbar);
        ActionBar ab = getSupportActionBar();
        if(ab != null) ab.setDisplayHomeAsUpEnabled(true);

        TextView tvName = findViewById(R.id.tv_name);
        TextView tvAddrs = findViewById(R.id.tv_address);


        ArrayList<String> info = getIntent().getStringArrayListExtra("station_mapinfo");
        log.i("Station Info: %s %s %s %s", info.get(0), info.get(1), info.get(2), info.get(3));
        tvName.setText(info.get(0));
        tvAddrs.setText(info.get(1));

    }
}
