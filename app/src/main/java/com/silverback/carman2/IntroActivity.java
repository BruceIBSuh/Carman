package com.silverback.carman2;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.OpinetDistCodeTask;
import com.silverback.carman2.threads.OpinetPriceTask;
import com.silverback.carman2.threads.ThreadManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.Arrays;

public class IntroActivity extends BaseActivity implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(IntroActivity.class);

    // Objects
    private OpinetPriceTask opinetPriceTask;
    private OpinetDistCodeTask opinetDistCodeTask;

    // UI's
    private ProgressBar mProgBar;

    // Fields
    private String jsonString;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar
        //getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        //getSupportActionBar().hide();

        setContentView(R.layout.activity_intro);

        // Set screen to portrait as indicated with "android:screenOrientation="portrait" in Manifest.xml
        // android:screenOrientation is not allowed with Android O_MR1 +
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mSettings = getSharedPreferenceInstance(this);

        // UI's and event handlers
        mProgBar = findViewById(R.id.progbar);
        findViewById(R.id.btn_start).setOnClickListener(this);

        File distCodePath = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
        if(!distCodePath.exists()) firstInitProcess();
        else initProcess();

    }

    // Invokes this method only once at launching for the first time in order to have the ImageView
    // size for a cover image
    private void firstInitProcess() {

        // Unless the district code has been saved, download again the sigun code list and save it.
        File distCode = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
        if(!distCode.exists()) opinetDistCodeTask = ThreadManager.startOpinetDistCodeTask(this);

        // Initial District
        String[] district = getResources().getStringArray(R.array.default_district);
        jsonString = new JSONArray(Arrays.asList(district)).toString();
        mSettings.edit().putString(Constants.DISTRICT, jsonString).apply();

    }

    private void initProcess() {

    }


    @Override
    public void onClick(View view) {
        log.i("start button clicked: %s", view.getId());
        mProgBar.setVisibility(View.VISIBLE);

        //if(checkUpdateOpinet()) {
            try {
                String jsonString = mSettings.getString(Constants.DISTRICT, "");
                JSONArray jsonArray = new JSONArray(jsonString);

                String sigunCode = jsonArray.get(2).toString();
                log.i("Sigun Code: %s", sigunCode);

                // Starts multi-threads(ThreadPoolExecutor) to download the opinet price info.
                // Consider whether the threads should be interrupted or not.
                for (int i = 0; i < 3; i++)
                    opinetPriceTask = ThreadManager.startOpinetPriceTask(IntroActivity.this, sigunCode, i);

                // Save the last update time in the default SharedPreferences
                mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
            } catch (JSONException e) {
                log.e("JSONException: %s", e);
            }
        //} else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        //}

    }


}
