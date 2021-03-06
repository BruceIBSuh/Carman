package com.silverback.carman;

/*
 * Copyright (c) 2020 SilverBack Trust
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at*
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.silverback.carman.backgrounds.NetworkStateWorker;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public abstract class BaseActivity extends AppCompatActivity {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BaseActivity.class);

    // Objects
    protected ThreadManager2 sThreadManager;
    protected CarmanDatabase sDB;
    protected String userId;
    protected SharedPreferences mSettings;
    protected static DecimalFormat df;

    // Fields
    protected boolean isNetworkConnected;

    // Implemented by checkRuntimePermission callback to check a specific permission.
    public interface PermissionCallback {
        void performAction();
    }

    // Runtime Permission using RequestPermission contract
    private final ActivityResultLauncher<String> reqPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), this::getPermissionResult);

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set screen to portrait as indicated with "android:screenOrientation="portrait" in Manifest.xml
        // for each activity. android:screenOrientation is not allowed with Android O_MR1
        if(Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1)
            super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Create the Work Thread
        sThreadManager = ThreadManager2.getInstance();
        sDB = CarmanDatabase.getDatabaseInstance(getApplicationContext());
        if(mSettings == null) mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        //jsonDistrict = mSettings.getString(Constants.DISTRICT, null);
        userId = getUserIdFromStorage(this);
        log.i("userId in BaseActivity: %s", userId);

        // Checkk if the network connectivitis ok.
        isNetworkConnected = notifyNetworkConnected(this);
    }

    public void checkRuntimePermission(
            View rootView, String perm, String rationale, PermissionCallback callback){
        if(ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            callback.performAction();
        } else if(ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
            Snackbar.make(rootView, rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", v -> reqPermissionLauncher.launch(perm))
                    .show();
        } else reqPermissionLauncher.launch(perm);
    }
    // Abstract method to get a permission result in each activity which extends this BaseActivity.
    public abstract void getPermissionResult(Boolean isPermitted);

    // Check a state of the network
    public static boolean notifyNetworkConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        log.i("network status: %s", networkInfo);
        return networkInfo != null && networkInfo.isConnected();
        //return connManager.isActiveNetworkMetered();
    }

    public WorkRequest requestNetworkConnectedWork() {
        // WorkManager to check the network connectivity before querying posts from Firestore.
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        return new OneTimeWorkRequest.Builder(NetworkStateWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR, //BackoffPolicy.Exponential.
                        OneTimeWorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS, // 10 seconds
                        TimeUnit.MILLISECONDS)
                .addTag("postingQuery")
                .build();
    }

    // Get the district name and code from SharedPreferences which saves them as type of JSONString
    // because it cannot contain any array generics.
    public JSONArray getDistrictJSONArray() {
        try {
            return new JSONArray(mSettings.getString(Constants.DISTRICT,
                    Arrays.asList(getResources().getStringArray(R.array.default_district)).toString()));
        } catch(JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // DefaultParams: fuelCode, radius to locate, sorting radius
    public final String[] getNearStationParams() {
        String[] defaultParams = new String[3];
        defaultParams[0] = mSettings.getString(Constants.FUEL, "B027");
        defaultParams[1] = mSettings.getString(Constants.SEARCHING_RADIUS, "2500");
        defaultParams[2] = mSettings.getString(Constants.ORDER, "2");

        return defaultParams;
    }

    public DecimalFormat getDecimalFormat() {
        DecimalFormat df = (DecimalFormat)NumberFormat.getInstance(Locale.getDefault());
        df.applyPattern("#,###");
        df.setDecimalSeparatorAlwaysShown(false);
        return df;
    }

    public static DecimalFormat getDecimalFormatInstance() {
        if(df == null) {
            df = (DecimalFormat) NumberFormat.getInstance(Locale.KOREAN);
            df.applyPattern("#,###");
            df.setDecimalSeparatorAlwaysShown(false);
        }
        return df;
    }


    // Formats date and time with milliseconds
    public static String formatMilliseconds(String format, long milliseconds) {
        //Date date = new Date(milliseconds);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(milliseconds);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(calendar.getTime());

    }

    public static long parseDateTime(String format, String datetime) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        try {
            Date date = sdf.parse(datetime);
            return date != null ? date.getTime() : 0;
        } catch(ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Check the time interval between the current time and the last update time saved in
    // SharedPreferences under the name of "Constants.OPINET_UPDATE_TIME", and the extra boolean
    // value passed from SettingsActivity that is true when the DistCode has changed,
    // then decide whether to newly update for the opinet price list or not
    // Big Bug here
    protected boolean checkPriceUpdate() {
        long currentTime = System.currentTimeMillis();
        long lastUpdate = mSettings.getLong(Constants.OPINET_LAST_UPDATE, 0L);
        log.i("opinet update: %s, %s, %s", currentTime, lastUpdate, currentTime - lastUpdate);
        return (currentTime - lastUpdate) > Constants.OPINET_UPDATE_INTERVAL;
    }

    // Measures the size of an android attribute based on ?attr/actionBarSize
    public float getActionbarHeight() {
        TypedValue typedValue = new TypedValue();
        if(getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimension(typedValue.data, getResources().getDisplayMetrics());
        }
        return -1;
    }

    // Match the gas station with its logo image by the station code.
    public static int getGasStationImage(String name) {
        int resId = -1;
        switch(name) {
            case "SKE": resId = R.drawable.logo_sk; break;
            case "GSC": resId = R.drawable.logo_gs; break;
            case "HDO": resId = R.drawable.logo_hyundai; break;
            case "SOL": resId = R.drawable.logo_soil; break;
            case "RTO": resId = R.drawable.logo_pb; break;
            case "RTX": resId = R.drawable.logo_express; break;
            case "NHO": resId = R.drawable.logo_nonghyup; break;
            case "E1G": resId = R.drawable.logo_e1g; break;
            case "SKG": resId = R.drawable.logo_skg; break;
            case "ETC": resId = R.drawable.logo_anonym; break;
            default: break;
        }

        return resId;
    }

    public static JSONObject createJsonItemObject(String name, int mileage, int period) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("mileage", mileage);
        jsonObject.put("month", period);

        return jsonObject;
    }

    // Default service items and check period and time span(premise: 10000 km / year)
    public JSONArray setDefaultServiceItems() {
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray.put(0, createJsonItemObject("???????????? ??? ????????????", 5000, 6));
            jsonArray.put(1, createJsonItemObject("???????????????", 5000, 6));
            jsonArray.put(2, createJsonItemObject("????????? ??????", 3000, 6));
            jsonArray.put(3, createJsonItemObject("????????? ??????", 5000, 6));
            jsonArray.put(4, createJsonItemObject("?????????", 10000, 12));
            jsonArray.put(5, createJsonItemObject("???????????????", 10000, 12));
            jsonArray.put(6, createJsonItemObject("????????? ?????? ??????", 25000, 30));
            jsonArray.put(7, createJsonItemObject("????????? ??????", 50000, 60));
            jsonArray.put(8, createJsonItemObject("???????????? ??????", 20000, 24));
            jsonArray.put(9, createJsonItemObject("???????????? ?????????", 10000, 12));
            jsonArray.put(10, createJsonItemObject("????????? ??????", 70000, 84));
            jsonArray.put(11, createJsonItemObject("????????????????????? ??????", 50000, 60));
            jsonArray.put(12, createJsonItemObject("???????????????", 70000, 84));
            return jsonArray;

        } catch(JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // The document id with which user data is uploaded to Firestore is used as USER ID. The Firebase
    // Auth id is not used for a security reason. Open and close file is so expensive that BaseActivity
    // opens the file and assign it to "userId" variable which is inherited to other activities.
    // Keep it in mind that the API 7 and above supports file-based encryption(FBE) and Android 10
    // and higher, FBE is required so that the code should be refactored at some time.
    public String getUserIdFromStorage(Context context) {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = context.openFileInput("userId");
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            String line = br.readLine();
            while(line != null) {
                sb.append(line);
                line = br.readLine();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }


    public SharedPreferences getSharedPreferernces() {
        return mSettings;
    }


}
