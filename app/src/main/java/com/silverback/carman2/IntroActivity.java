package com.silverback.carman2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProviders;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.GasPriceTask;
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

/**
 * This class downloads necessary resources not only from the server but also from the local db and
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
    private DistrictCodeTask distCodeTask;
    private OpinetViewModel opinetViewModel;
    private File distCodeFile;

    // UI's
    private ProgressBar mProgBar;

    // Fields
    private String stnId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Instantiate the objects
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        mDB = CarmanDatabase.getDatabaseInstance(this);
        opinetViewModel = ViewModelProviders.of(this).get(OpinetViewModel.class);

        mProgBar = findViewById(R.id.progbar);
        // Fork the process into the first-time launching or the regular one based upon whether the
        // Firebase anonymous authentication
        findViewById(R.id.btn_start).setOnClickListener(view -> {
            if(mAuth.getCurrentUser() == null) firstInitProcess();
            else regularInitProcess();
        });


        // Notified of the dist
        opinetViewModel.distCodeComplete().observe(this, isComplete -> {
            if(isComplete) {
                mProgBar.setVisibility(View.INVISIBLE);
                regularInitProcess();
            } else {
                Toast.makeText(this, "District Code failed to fetch", Toast.LENGTH_SHORT).show();
            }
        });

        // Being notified of the opinet price data on the worker thread of GasPriceTask,
        // start MainActivity and finish the progressbar
        opinetViewModel.distPriceComplete().observe(this, isDone -> {
            log.i("district price complete");
            // Save the last update time in SharedPreferences
            mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
            mProgBar.setVisibility(View.GONE);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    // Required to stop the worker threads in ThreadManager, not here!
    @Override
    public void onPause() {
        super.onPause();
        if(distCodeTask != null) distCodeTask = null;
        if(gasPriceTask != null) gasPriceTask = null;
    }

    // The single-time process at the first-time launching
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
                    try (final FileOutputStream fos = openFileOutput("userId", Context.MODE_PRIVATE)) {
                        fos.write(id.getBytes());
                    } catch (IOException e) {
                        log.e("IOException: %s", e.getMessage());
                    }
                }).addOnFailureListener(e -> log.e("Add user failed: %s", e.getMessage()));

                // Initiate the task to get the district codes provided by Opinet and save them in
                // the internal storage. It may be replaced by downloading it from the server  every
                // time the app starts for decreasing the app size
                distCodeFile = new File(getFilesDir(), Constants.FILE_DISTRICT_CODE);
                if(!distCodeFile.exists())
                    distCodeTask = ThreadManager.saveDistrictCodeTask(this, opinetViewModel);

                // Retrieve the default district values of sido, sigun and sigun code from resources,
                // then save them in SharedPreferences.
                JSONArray jsonDistrictArray = new JSONArray(
                        Arrays.asList(getResources().getStringArray(R.array.default_district)));
                mSettings.edit().putString(Constants.DISTRICT, jsonDistrictArray.toString()).apply();

                // Retrive the default service items as JSONArray defined in BaseActvitiy and save
                // them in SharedPreferences. It may be also replaced by downloading from the server
                JSONArray jsonServiceItemArray = BaseActivity.getJsonServiceItemArray();
                mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonServiceItemArray.toString()).apply();

            } else {
                log.i("Anonymous Authentication failed");
            }
        });

    }

    private void regularInitProcess() {
        mProgBar.setVisibility(View.VISIBLE);

        File file = new File(getCacheDir(), Constants.FILE_CACHED_AVG_PRICE);
        String distCode;

        // The price check time set in Constants.OPINET_UPDATE_INTERVAL has lapsed
        // or no file as to the average oil price exists b/c it is the first-time launching.
        //if(checkPriceUpdate() || !file.exists()) {
        if(checkPriceUpdate() || !file.exists()) {
            log.i("Receiving the oil price");
            List<String> district = convJSONArrayToList();
            if(district == null) distCode = "0101";
            else distCode = district.get(2);

            // Initiate the task to retrieve the prices of average, sido, sigun district from
            // the Opinet server, then notify this activity of having the data done by OpinetViewModel.
            // distPriceComplete() defined in onCreate() to move to MainActivity.
            gasPriceTask = ThreadManager.startGasPriceTask(this, opinetViewModel, distCode);

            /*
            // Retrieve the first-set gas station and service center in each placeholders from DB
            // to fetch the id which is used as param to get the price, then pass it to MainActivity.
            mDB.favoriteModel().getFirstFavorite(Constants.GAS).observe(this, id -> {
                log.i("Intro StationID: %s", stnId);
                gasPriceTask = ThreadManager.startGasPriceTask(this, opinetViewModel, distCode, stnId);
            });

             */

        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }


}
