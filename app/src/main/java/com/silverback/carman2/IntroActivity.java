package com.silverback.carman2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.PriceTask;
import com.silverback.carman2.threads.SaveDistCodeTask;
import com.silverback.carman2.threads.ThreadManager;

import org.json.JSONArray;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class IntroActivity extends BaseActivity implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(IntroActivity.class);

    // Objects
    private PriceTask priceTask;
    private SaveDistCodeTask saveDistCodeTask;

    // UI's
    private ProgressBar mProgBar;

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

        // Downloads the district code provided by Opinet and saves it in the designated locaiton
        // when initially running the app or finding the code changed later.
        File distCodePath = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
        if(!distCodePath.exists()) firstInitProcess();
    }

    @Override
    public void onPause() {
        super.onPause();
        //if(saveDistCodeTask != null) saveDistCodeTask = null;
        //if(priceTask != null) priceTask = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if(saveDistCodeTask != null) saveDistCodeTask = null;
        if(priceTask != null) priceTask = null;
    }

    @Override
    public void onClick(View view) {

        mProgBar.setVisibility(View.VISIBLE);
        File file = new File(getCacheDir(), Constants.FILE_CACHED_AVG_PRICE);
        String distCode = null;

        if(checkUpdateOilPrice() || !file.exists()) {

            List<String> district = convJSONArrayToList();
            if(district == null) distCode = "0101";
            else distCode = district.get(2);

            // Starts multi-threads(ThreadPoolExecutor) to download the opinet price info.
            // Consider whether the threads should be interrupted or not.
            priceTask = ThreadManager.startPriceTask(IntroActivity.this, distCode);

            // Save the last update time in SharedPreferences
            mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();

        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    // Invokes this method only once at launching for the first time in order to have the ImageView
    // size for a cover image
    @SuppressWarnings("ConstantConditions")
    private void firstInitProcess() {

        // Unless the district code has been saved, download again the sigun code list and save it.
        log.i("firstInitProcess: download District Code");
        File distCode = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
        if(!distCode.exists()) saveDistCodeTask = ThreadManager.downloadOpinetDistCodeTask(this);

        // Retrieve the default district values of sido, sigun and sigun code, then save them in
        // SharedPreferences.
        JSONArray jsonDistrictArray = new JSONArray(Arrays.asList(getResources().getStringArray(R.array.default_district)));
        JSONArray jsonServiceItemArray = BaseActivity.getJsonServiceItemArray();

        mSettings.edit().putString(Constants.DISTRICT, jsonDistrictArray.toString()).apply();
        mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonServiceItemArray.toString()).apply();

    }

    // Invoked by ThreadManager when the average, sido, and sigun oil prices have been retrieved
    // on working threads.
    public void onPriceTaskComplete() {
        log.i("PriceTask complete");
        if(priceTask != null) priceTask = null;
        mProgBar.setVisibility(View.GONE);
        startActivity(new Intent(IntroActivity.this, MainActivity.class));
        finish();
    }

}
