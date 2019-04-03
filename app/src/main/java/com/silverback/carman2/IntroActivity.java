package com.silverback.carman2;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.SaveDistCodeTask;
import com.silverback.carman2.threads.PriceTask;
import com.silverback.carman2.threads.ThreadManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.Arrays;

public class IntroActivity extends BaseActivity implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(IntroActivity.class);

    // Objects
    private PriceTask priceTask;
    private SaveDistCodeTask saveDistCodeTask;

    // UI's
    private ProgressBar mProgBar;

    // Fields
    private String jsonString;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar
        //getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_intro);

        // Permission Check
        checkPermissions();

        // UI's and event handlers
        mProgBar = findViewById(R.id.progbar);
        findViewById(R.id.btn_start).setOnClickListener(this);

        File distCodePath = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
        if(!distCodePath.exists()) firstInitProcess();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(saveDistCodeTask != null) saveDistCodeTask = null;
        if(priceTask != null) priceTask = null;
    }

    @Override
    public void onClick(View view) {

        mProgBar.setVisibility(View.VISIBLE);

        if(checkUpdateOpinet()) {
            try {
                String distCode = mSettings.getString(Constants.DISTRICT, "0101");
                JSONArray jsonArray = new JSONArray(distCode);

                String sigunCode = jsonArray.get(2).toString();
                log.i("Sigun Code: %s", "0101");

                // Starts multi-threads(ThreadPoolExecutor) to download the opinet price info.
                // Consider whether the threads should be interrupted or not.
                priceTask = ThreadManager.startPriceTask(IntroActivity.this, sigunCode);

                // Save the last update time in SharedPreferences
                mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();

            } catch (JSONException e) {
                log.e("JSONException: %s", e);
            }

        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    // Invokes this method only once at launching for the first time in order to have the ImageView
    // size for a cover image
    private void firstInitProcess() {

        // Unless the district code has been saved, download again the sigun code list and save it.
        File distCode = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
        if(!distCode.exists()) saveDistCodeTask = ThreadManager.downloadOpinetDistCodeTask(this);

        // Initial District
        String[] district = getResources().getStringArray(R.array.default_district);
        jsonString = new JSONArray(Arrays.asList(district)).toString();
        mSettings.edit().putString(Constants.DISTRICT, jsonString).apply();

    }

    // Invoked by ThreadManager when the average, sido, and sigun oil prices have been retrieved
    // on working threads.
    public void onPriceComplete() {
        if(priceTask != null) priceTask = null;
        mProgBar.setVisibility(View.GONE);
        startActivity(new Intent(IntroActivity.this, MainActivity.class));
        finish();
    }

}
