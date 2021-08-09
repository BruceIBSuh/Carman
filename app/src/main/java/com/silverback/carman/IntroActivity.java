package com.silverback.carman;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.databinding.ActivityIntroBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.DistCodeDownloadTask;
import com.silverback.carman.threads.GasPriceTask;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.OpinetViewModel;

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
 * The first initiation downloads the district code from the Opinet server in the worker thread
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
    private static final LoggingHelper log = LoggingHelperFactory.create(IntroActivity.class);

    // Objects
    private ActivityIntroBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private CarmanDatabase mDB;
    private GasPriceTask gasPriceTask;
    private DistCodeDownloadTask distCodeTask;
    private OpinetViewModel opinetModel;
    private String[] defaultDistrict;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Instantiate objects.
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        mDB = CarmanDatabase.getDatabaseInstance(this); // going to be replace with sDB in BaseActivity
        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);

        // Retrieve resources.
        defaultDistrict = getResources().getStringArray(R.array.default_district);

        // On clicking the start button, fork the process into the first-time launching or the regular
        // process depending upon whether the Firebase anonymous authentication is registered.
        binding.btnStart.setOnClickListener(view -> {
            binding.pbIntro.setVisibility(View.VISIBLE);
            if(mAuth.getCurrentUser() == null) firstInitProcess();
            else regularInitProcess();
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        //if(distCodeTask != null) distCodeTask = null;
        //if(gasPriceTask != null) gasPriceTask = null;
    }


    // Invoked when and only when the application runs for the first time, authenticating the user
    // in Firebase.Auth. Once authenticated, upload the user to the "user" collection with data null
    // at the moment, then have the document id saved in the internal storage. The user document ID
    // is used to identify users instead of using Firebase.Auth UID for security reason. Finally,
    // initiate DistCodeTask to retrieve the district codes from the Opinet.
    //
    // Refactor requried: JSONARray as to the sido code and the service items currently defined as
    // resources and saved in SharedPreferences should be refactored to download directly from the server.
    @SuppressWarnings("ConstantConditions")
    private void firstInitProcess() {
        mAuth.signInAnonymously().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                log.i("current user:%s", mAuth.getUid());
                Map<String, Object> userData = new HashMap<>();
                userData.put("user_id", mAuth.getUid());
                userData.put("user_name", null);
                userData.put("user_pic", null);
                userData.put("auto_data", null);

                firestore.collection("users").add(userData).addOnSuccessListener(docref -> {
                    final String userId = docref.getId();
                    if(!TextUtils.isEmpty(userId)) {
                        try (FileOutputStream fos = openFileOutput("userId", Context.MODE_PRIVATE)) {
                            fos.write(userId.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).addOnFailureListener(Exception::printStackTrace);

                // Retrieve the default district values of sido, sigun and sigun code from resources,
                // then save them in SharedPreferences.
                JSONArray jsonDistrictArray = new JSONArray(Arrays.asList(
                        getResources().getStringArray(R.array.default_district)));
                mSettings.edit().putString(Constants.DISTRICT, jsonDistrictArray.toString()).apply();

                // Retrive the default service items as JSONArray defined in BaseActvitiy and save
                // them in SharedPreferences. It may be also replaced by downloading from the server
                JSONArray jsonServiceItemArray = BaseActivity.getJsonServiceItemArray();
                mSettings.edit().putString(Constants.SERVICE_ITEMS, jsonServiceItemArray.toString()).apply();

                // Initiate DistrictCodeTask to get the district codes provided by Opinet and save
                // them in the internal storage. It may be replaced by downloading it from the server
                // every time the app starts for decreasing the app size
                distCodeTask = sThreadManager.saveDistrictCodeTask(this, opinetModel);
                // Notified of having the district codes(sigun codes) complete, which was running in the
                // background by DistrictCodeTask only during firstInitProcess().
                opinetModel.distCodeComplete().observe(this, isComplete -> {
                    try {
                        if (isComplete) {
                            binding.pbIntro.setVisibility(View.INVISIBLE);
                            regularInitProcess();
                        } else throw new FileNotFoundException();
                    } catch(FileNotFoundException e) { e.printStackTrace();}
                });
            }
        });

    }

    // Once the user is authenticated by Firebase.Auth, this process is invoked to check whether the
    // price data should be updated. The price update runs in the background using GasPriceTask with
    // OpinetViewModel which returns the result value. The first placeholder of the favorite will be
    // retrieved from the Room database.
    private void regularInitProcess() {
        // Check if the price updating interval set in Constants.OPINET_UPDATE_INTERVAL, has elapsed.
        // As GasPriceTask completes, updated prices is notified by calling OpinetViewModel.distPriceComplete().
        if(checkPriceUpdate()) {
            // Get the sigun code
            JSONArray json = getDistrictJSONArray();
            String distCode = (json == null) ? defaultDistrict[2] : json.optString(2);
            mDB.favoriteModel().getFirstFavorite(Constants.GAS).observe(this, stnId -> {
                //JSONArray json = BaseActivity.getDistrictJSONArray();
                //String distCode = (json != null) ? json.optString(2) : defaultDistrict[2];
                gasPriceTask = sThreadManager.startGasPriceTask(this, opinetModel, distCode, stnId);
                // Notified of having each price of average, sido, sigun and the first placeholder of the
                // favorite, if any, fetched from the Opinet by GasPriceTask, saving the current time in
                // SharedPreferences to check whether the price should be updated for the next initiation.
                opinetModel.distPriceComplete().observe(this, isDone -> {
                    mSettings.edit().putLong(Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
                    startActivity(new Intent(this, MainActivity.class));
                    binding.pbIntro.setVisibility(View.GONE);
                    finish();
                });
            });

        } else {
            startActivity(new Intent(this, MainActivity.class));
            binding.pbIntro.setVisibility(View.GONE);
            finish();
        }


    }
}
