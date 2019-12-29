package com.silverback.carman2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.lifecycle.ViewModelProviders;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.PriceDistrictTask;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.threads.DistrictCodeTask;
import com.silverback.carman2.threads.ThreadManager;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntroActivity extends BaseActivity implements View.OnClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(IntroActivity.class);

    // Objects
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private PriceDistrictTask priceDistrictTask;
    private DistrictCodeTask districtCodeTask;
    private OpinetViewModel opinetViewModel;

    // UI's
    private ProgressBar mProgBar;

    // Fields
    private String stnId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the action bar
        //getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_intro);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        // Permission Check
        checkPermissions();

        // UI's and event handlers
        mProgBar = findViewById(R.id.progbar);
        findViewById(R.id.btn_start).setOnClickListener(this);


        // Downloads the district code provided by Opinet and saves it in the designated locaiton
        // when initially running the app or finding the code changed later.
        /*
        File distCodePath = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
        if(!distCodePath.exists()) firstInitProcess();
        */

        opinetViewModel = ViewModelProviders.of(this).get(OpinetViewModel.class);
        // On finishing saving the district code which was downloaded and saved,
        opinetViewModel.districtCodeComplete().observe(this, isCompete -> {
            log.i("District code task finished");
            mProgBar.setVisibility(View.INVISIBLE);
            initProcess();
        });

        // Being notified of the opinet price data on the worker thread of PriceDistrictTask,
        // start MainActivity and finish the progressbar
        opinetViewModel.districtPriceComplete().observe(this, isComplete -> {
            log.i("PriceDistrictTask complete");
            if(priceDistrictTask != null) priceDistrictTask = null;
            mProgBar.setVisibility(View.GONE);
            startActivity(new Intent(IntroActivity.this, MainActivity.class));
            finish();
        });

        // Retrieve the providers set at the first in the favorite list in order to get the price
        // of the gas station and display the favorite price view.
        CarmanDatabase.getDatabaseInstance(this).favoriteModel().queryFirstSetFavorite().observe(this, data -> {
            for(FavoriteProviderDao.FirstSetFavorite provider : data) {
                if(provider.category == Constants.GAS) stnId = provider.providerId;
                log.i("Station ID: %s", stnId);
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    // Required to stop the worker threads in ThreadManager, not here!
    @Override
    public void onPause() {
        super.onPause();
        if(districtCodeTask != null) districtCodeTask = null;
        if(priceDistrictTask != null) priceDistrictTask = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if(districtCodeTask != null) districtCodeTask = null;
        if(priceDistrictTask != null) priceDistrictTask = null;
    }

    @Override
    public void onClick(View view) {
        // Fork the process into the first and regular based on whether the anonymous
        if(mAuth.getCurrentUser() == null) firstInitProcess();
        else initProcess();
    }

    // The single-time process at the first launching
    @SuppressWarnings("ConstantConditions")
    private void firstInitProcess() {
        mProgBar.setVisibility(View.VISIBLE);
        // Anonymous Authentication with Firebase.Auth
        mAuth.signInAnonymously().addOnCompleteListener(task -> {

            if(task.isSuccessful()) {

                // Create "users" collection and the fields with temporary null values in Firestore,
                // retrieving the generated id, which is saved in the internal storage.
                Map<String, Object> userData = new HashMap<>();
                userData.put("user_name", null);
                userData.put("user_pic", null);
                userData.put("auto_data", null);

                firestore.collection("users").add(userData).addOnSuccessListener(docref -> {
                    final String id = docref.getId();
                    // Get the document id generated by Firestore and save it in the internal storage.
                    try (final FileOutputStream fos = openFileOutput("userId", Context.MODE_PRIVATE)) {
                        fos.write(id.getBytes());
                    } catch (IOException e) {
                        log.e("IOException: %s", e.getMessage());
                    }
                }).addOnFailureListener(e -> log.e("Add user failed: %s", e.getMessage()));

                // Initiate the task to get the district codes provided by Opinet and save them in
                // the internal storage, which may be downloaded every time the app starts to
                // decrease the app size.
                File distCode = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
                if(!distCode.exists())
                    districtCodeTask = ThreadManager.saveDistrictCodeTask(this, opinetViewModel);

                // Retrieve the default district values of sido, sigun and sigun code, then save them in
                // SharedPreferences.
                JSONArray jsonDistrictArray = new JSONArray(
                        Arrays.asList(getResources().getStringArray(R.array.default_district)));
                mSettings.edit().putString(Constants.DISTRICT, jsonDistrictArray.toString()).apply();

                // Retrive the default service items as JSONArray defined in BaseActvitiy and save
                // them in SharedPreferences
                JSONArray jsonServiceItemArray = BaseActivity.getJsonServiceItemArray();
                mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonServiceItemArray.toString()).apply();

            } else {
                log.i("Anonymous Authentication failed");
            }
        });

    }

    private void initProcess() {

        mProgBar.setVisibility(View.VISIBLE);
        File file = new File(getCacheDir(), Constants.FILE_CACHED_AVG_PRICE);
        String distCode;

        if(checkUpdateOilPrice() || !file.exists()) {
            log.i("Receiving the oil price");
            List<String> district = convJSONArrayToList();
            if(district == null) distCode = "0101";
            else distCode = district.get(2);

            // Starts multi-threads(ThreadPoolExecutor) to download the opinet price info.
            // Consider whether the threads should be interrupted or not.
            priceDistrictTask = ThreadManager.startPriceDistrictTask(this, opinetViewModel, distCode, stnId);
            //priceDistrictTask = ThreadManager.startPriceDistrictTask(this, opinetViewModel, distCode);

            // Save the last update time in SharedPreferences
            mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();

        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

}
