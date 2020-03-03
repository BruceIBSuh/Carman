package com.silverback.carman2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.AutoDataResourceTask;
import com.silverback.carman2.threads.GasPriceTask;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.threads.DistrictCodeTask;
import com.silverback.carman2.threads.ThreadManager;

import org.json.JSONArray;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
 * This class downloads necessary resources not only from the server but also from the local db, then
 * starts the app. The process is forked as the first time initiation and the regular initiation,
 * according to whether the user is anonymously registered with Firebase anonymous authentication.
 *
 * The first time initiation downloads the district code from the Opinet server in the worker thread
 * (DistCodeTask), the result of which is back here by OpinetViewModel.distCodeComplete(boolean).
 * No matter what is the return value, it continues to the regular process, retrying to download
 * the code when the relevant spinner adapter requires to the code.
 *
 * Once initiating the reuglar process, check whether the update interval lapses or the file saving
 * the average price exists. If either of those conditions is not satisfied, make the process retrieve
 * the price data concerning avgerage, sido, sigun and the favorite gas station from the local db,
 * if any, from the Opinet Server using the worker thread(GasPriceTask), the result value of which is
 * sent back via OpinetViweModel.distPriceComplete(boolean) and saves the update time in SharedPreferences.
 *
 * Otherwise, the process goes to MainActivity and the price data should be fetched from the file
 * saved in the internal storage.
 */

public class IntroActivity extends BaseActivity  {
    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(IntroActivity.class);
    // Objects
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private CarmanDatabase mDB;
    private GasPriceTask gasPriceTask;
    private AutoDataResourceTask autoDataResourceTask;
    private DistrictCodeTask distCodeTask;
    private OpinetViewModel opinetViewModel;
    private FirestoreViewModel firestoreViewModel;
    private String[] defaultDistrict;

    // UI's
    private ProgressBar mProgBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        defaultDistrict = getResources().getStringArray(R.array.default_district);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        mDB = CarmanDatabase.getDatabaseInstance(this);
        opinetViewModel = new ViewModelProvider(this).get(OpinetViewModel.class);
        firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);

        mProgBar = findViewById(R.id.pb_intro);
        // On clicking the start button, fork the process into the first-time launching or the regular
        // process depending upon whether the Firebase anonymous authentication is registered.

        ImageButton btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(view -> {
            mProgBar.setVisibility(View.VISIBLE);
            log.i("FirebaseAuth: %s", mAuth.getCurrentUser());
            if(mAuth.getCurrentUser() == null) firstInitProcess();
            else regularInitProcess();
        });

        /*
         * REFACTOR REQURIRED
         * Initial tasks should be done at a time by initiating the same task with multiple
         * runnables.
         */
        // Notified of having the district codes(sigun codes) complete, which was running in the
        // background by DistrictCodeTask only during firstInitProcess().
        opinetViewModel.distCodeComplete().observe(this, isComplete -> {
            try {
                if (isComplete) {
                    //mProgBar.setVisibility(View.INVISIBLE);
                    autoDataResourceTask = ThreadManager.startFirestoreResTask(this, firestoreViewModel);
                    //regularInitProcess();
                } else throw new FileNotFoundException();

            } catch(FileNotFoundException e) {
                log.e("District Code FileNotFoundException: %s", e.getMessage());
            }
        });

        // Notified of having completed to download auto data resources and to save it in the file.
        firestoreViewModel.getAutoResourceTaskDone().observe(this, isDone -> {
            mProgBar.setVisibility(View.INVISIBLE);
            regularInitProcess();
        });

        // Notified of having each price of average, sido, sigun and the first placeholder of the
        // favorite, if any, fetched from the Opinet by GasPriceTask, saving the current time in
        // SharedPreferences to check whether the price should be updated for the next initiation.
        opinetViewModel.distPriceComplete().observe(this, isDone -> {
            mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
            startActivity(new Intent(this, MainActivity.class));
            mProgBar.setVisibility(View.GONE);
            finish();
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        if(distCodeTask != null) distCodeTask = null;
        if(gasPriceTask != null) gasPriceTask = null;
        if(autoDataResourceTask != null) autoDataResourceTask = null;
    }


    // Invoked when and only when the application initiates to authenticate the user in Firebase.Auth.
    // On authentication, add the user to the collection named "users" with the user data temporarily
    // as null and have the document id saved in the internal storage. Finally, initiate DistCodeTask
    // to retrieve the district codes from the Opinet.
    // Refactor requried: JSONARray as to the sido code and the service items currently defined as
    // resources and saved in SharedPreferences should be refactored to download directly from the server.
    @SuppressWarnings("ConstantConditions")
    private void firstInitProcess() {
        //mProgBar.setVisibility(View.VISIBLE);
        // Anonymous Authentication with Firebase.Auth
        mAuth.signInAnonymously().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("user_name", null);
                userData.put("user_pic", null);
                userData.put("auto_data", null);

                firestore.collection("users").add(userData).addOnSuccessListener(docref -> {
                    final String id = docref.getId();
                    try (final FileOutputStream fos = openFileOutput("userId", Context.MODE_PRIVATE)) {
                        fos.write(id.getBytes());
                    } catch (IOException e) {
                        log.e("IOException: %s", e.getMessage());
                    }
                }).addOnFailureListener(e -> log.e("Add user failed: %s", e.getMessage()));

                // Initiate DistrictCodeTask to get the district codes provided by Opinet and save
                // them in the internal storage. It may be replaced by downloading it from the server
                // every time the app starts for decreasing the app size
                distCodeTask = ThreadManager.saveDistrictCodeTask(this, opinetViewModel);

                // Retrieve the default district values of sido, sigun and sigun code from resources,
                // then save them in SharedPreferences.
                JSONArray jsonDistrictArray = new JSONArray(Arrays.asList(defaultDistrict));
                mSettings.edit().putString(Constants.DISTRICT, jsonDistrictArray.toString()).apply();

                // Retrive the default service items as JSONArray defined in BaseActvitiy and save
                // them in SharedPreferences. It may be also replaced by downloading from the server
                JSONArray jsonServiceItemArray = BaseActivity.getJsonServiceItemArray();
                mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonServiceItemArray.toString()).apply();

            } else {
                log.e("Anonymous Authentication failed");
            }
        });

    }

    // Once the user is authenticated by Firebase.Auth, this process is invoked to check whether the
    // price data should be updated. The price update runs in the background using GasPriceTask with
    // OpinetViewModel which returns the result value. The first placeholder of the favorite will be
    // retrieved from the Room database.
    private void regularInitProcess() {
        mProgBar.setVisibility(View.VISIBLE);
        // Check if the price updating interval, set in Constants.OPINET_UPDATE_INTERVAL, has lapsed.
        // As GasPriceTask completes, updated prices is notified as LiveData to OpinetViewModel.
        // distPriceComplete().
        if(checkPriceUpdate()) {
            mDB.favoriteModel().getFirstFavorite(Constants.GAS).observe(this, stnId -> {
                JSONArray json = BaseActivity.getDistrictJSONArray();
                String distCode = (json != null) ? json.optString(2) : defaultDistrict[2];
                log.i("District code: %s", distCode);
                gasPriceTask = ThreadManager.startGasPriceTask(this, opinetViewModel, distCode, stnId);
            });

        } else {
            startActivity(new Intent(this, MainActivity.class));
            mProgBar.setVisibility(View.GONE);
            finish();
        }
    }



}
