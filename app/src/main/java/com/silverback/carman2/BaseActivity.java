package com.silverback.carman2;

/**
 * Copyright (c) 2020 SilverBack
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

import android.Manifest;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.fragments.PermissionDialogFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity implements PermissionDialogFragment.OnDialogListener{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BaseActivity.class);

    // Constants
    protected static final int REQUEST_PERMISSION_FINE_LOCATION = 1000;
    protected static final int REQUEST_PERMISSION_BACKGROUND_LOCATION = 1001;
    protected static final int REQUEST_PERMISSION_CAMERA = 1002;

    // Objects
    protected String userId;
    protected static SharedPreferences mSettings;
    protected static DecimalFormat df;
    protected ApplyImageResourceUtil applyImageResourceUtil;

    // Fields
    private String permName;
    protected boolean isNetworkConnected;
    protected boolean isPermitted;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set screen to portrait as indicated with "android:screenOrientation="portrait" in Manifest.xml
        // android:screenOrientation is not allowed with Android O_MR1 +
        if(Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1)
            super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        if(mSettings == null) mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        //jsonDistrict = mSettings.getString(Constants.DISTRICT, null);
        userId = getUserIdFromStorage(this);

        // Checkk if the network connectivitis ok.
        isNetworkConnected = notifyNetworkConnected(this);
    }



    /**
     * ContextCompat.checkSelfPermission vs PermissionChecker
     * ContextCompat.checkSelfPermission is preferred in terms of its simplicity that it returns
     * PERMISSION_GRANTED or PERMISSION_DENITED. PermissionChecker, on the other hand, returns those
     * two values plus PERMISSION_DENITED_APP_OP, calling Context.checkPermission, then ApppOpsManager,
     * which seems to be intended for other apps in IPC environment
     */
    public void checkPermissions(String name) {
        permName = name;
        switch(name) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if(ContextCompat.checkSelfPermission(this, name) == PackageManager.PERMISSION_GRANTED) {
                    isPermitted = true;
                } else if(ActivityCompat.shouldShowRequestPermissionRationale(this, name)) {
                    String title = "Fine Location Permission";
                    String msg = "This permission is required to access the current location";
                    showPermissionRationale(title, msg);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{name}, Constants.REQUEST_PERMISSION_FINE_LOCATION);
                }

                break;

            // Permit BACKGROUND_LOCATION AT API 29(Android 10) or higher.
            case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                if(ContextCompat.checkSelfPermission(this, name) != PackageManager.PERMISSION_GRANTED) {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, name)) {
                        String title = "Backkground Locatioin Permission";
                        String msg = "Geofencing requires this permission to be feasible";
                        showPermissionRationale(title, msg);
                    } else {
                        log.i("request ACCESS_BACKGROUND_LOCATION");
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, name},
                                REQUEST_PERMISSION_BACKGROUND_LOCATION);
                    }


                }
                break;

            case Manifest.permission.CAMERA:
                if(ContextCompat.checkSelfPermission(this, name) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        ActivityCompat.requestPermissions(this, new String[]{name}, REQUEST_PERMISSION_CAMERA);
                }

                break;

            default: break;
        }

    }

    /*
     * Manage a request code yourself vs use RequestPermission contract included in AndroidX Library
     * (androidx.activity.1.2.0 alpha). Refactor should be made when the library releases an official
     * version.
     */
    /*
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {

        log.i("onrequestpermissionresult");
        switch (requestCode) {
            case Constants.REQUEST_PERMISSION_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    log.i("Access Fine Location permiited");
                    isPermitted = true;
                } else {
                    String title = "Location Permission Rejected";
                    String msg = "You have denied to access Location which disables";
                    //showPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION, title, msg);
                }

                break;

            case REQUEST_PERMISSION_BACKGROUND_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    log.i("background location granted");
                } else {
                    log.i("Access Background Location denied");
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        String title = "";
                        String msg = "";
                        //showPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION, title, msg);
                    }
                }

                break;

            case REQUEST_PERMISSION_CAMERA:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    log.i("Camera permitted");
                } else {
                    log.i("Camera denied");
                }

                break;
        }
    }

     */

    public void showPermissionRationale(String title, String msg) {
        DialogFragment rationaleFragment = new PermissionDialogFragment(this, title, msg);
        rationaleFragment.show(getSupportFragmentManager(), "rationaleFragment");
    }

    public boolean getPermission() {
        return isPermitted;
    }


    // Check a state of the network
    public static boolean notifyNetworkConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
        //return connManager.isActiveNetworkMetered();
    }

    // DefaultParams: fuelCode, radius to locate, sorting radius
    protected final String[] getDefaultParams() {

        String[] defaultParams = new String[3];
        defaultParams[0] = mSettings.getString(Constants.FUEL, "B027");
        defaultParams[1] = mSettings.getString(Constants.SEARCHING_RADIUS, "2500");
        defaultParams[2] = mSettings.getString(Constants.ORDER, "2");

        return defaultParams;
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



    // DecimalFormat method
    public static DecimalFormat getDecimalformatInstance() {
        if(df == null) {
            df = (DecimalFormat)NumberFormat.getInstance();
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
            //log.e("ParseException: %s", e.getMessage());
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
        return (currentTime - lastUpdate) > Constants.OPINET_UPDATE_INTERVAL;
    }



    // Reference method to get a debug Hashkey for Kakao
    // or by using Terminal,
    // keytool -exportcert -alias androiddebugkey -keystore <debug_keystore_path>
    // -storepass android -keypass android | openssl sha1 -binary | openssl base64
    /*
    protected void getHashKey() {
        try {
            PackageInfo info = getPackageManager()
                    .getPackageInfo("com.silverback.carman2", PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                log.i("KeyHash: %s", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    */


    public static DecimalFormat getDecimalFormatInstance() {
        if(df == null) {
            df = (DecimalFormat) NumberFormat.getInstance(Locale.KOREAN);
            df.applyPattern("#,###");
            df.setDecimalSeparatorAlwaysShown(false);
        }
        return df;
    }


    // Programatically, add titles and icons on the TabLayout, which must be invoked after
    // setupWithViewPager when it is linked to ViewPager.
    @SuppressWarnings("ConstantConditions")
    protected void addTabIconAndTitle(Context context, TabLayout tabLayout) {
        List<String> tabTitleList = null;
        List<Drawable> tabIconList = null;
        if(context instanceof ExpenseActivity) {
            tabTitleList = Arrays.asList(getResources().getStringArray(R.array.tab_carman_title));

            Drawable[] icons = {
                    getDrawable(R.drawable.ic_gas),
                    getDrawable(R.drawable.ic_service),
                    getDrawable(R.drawable.ic_stats)};
            tabIconList = Arrays.asList(icons);

        } else if(context instanceof BoardActivity) {
            tabTitleList = Arrays.asList(getResources().getStringArray(R.array.board_tab_title));
        }

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setText(tabTitleList.get(i));
            if(tabIconList != null) tabLayout.getTabAt(i).setIcon(tabIconList.get(i));
        }
    }

    // Slide up and down the TabLayout when clicking the buttons on the toolbar.
    /*
    protected boolean animSlideTabLayout(FrameLayout frame, TabLayout tabLayout, boolean isTabVisible) {
        float toolbarHeight = getActionbarHeight();
        float tabEndValue = (!isTabVisible)? toolbarHeight : 0;

        ObjectAnimator slideTab = ObjectAnimator.ofFloat(tabLayout, "y", tabEndValue);
        ObjectAnimator slideViewPager = ObjectAnimator.ofFloat(frame, "translationY", tabEndValue);
        slideTab.setDuration(1000);
        slideViewPager.setDuration(1000);
        slideTab.start();
        slideViewPager.start();

        return !isTabVisible;

    }
    */


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

    public static JSONArray getJsonServiceItemArray() {
        String jsonServiceItem =
                "[{\"name\":\"엔진오일 및 오일필터\",\"mileage\":8000,\"month\":6}," +
                "{\"name\":\"에어클리너\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"에어컨 필터\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"에어컨 가스\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"냉각수\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"얼라인먼트\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"타이어 위치 교환\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"타이어 교체\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"브레이크 패드\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"브레이크 라이닝\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"배터리 교체\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"트랜스미션오일 교체\",\"mileage\":5000,\"month\":6}," +
                "{\"name\":\"타이밍벨트 교체\",\"mileage\":5000,\"month\":6}]";

        try {
            return  new JSONArray(jsonServiceItem);
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

        return null;
    }

    public String getDefaultAutoFilter() {
        List<String> filterList = new ArrayList<>();
        filterList.add(getString(R.string.board_filter_brand));
        filterList.add(getString(R.string.board_filter_model));
        filterList.add(getString(R.string.board_filter_type));
        filterList.add(getString(R.string.board_filter_year));

        return new JSONArray(filterList).toString();
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


    @Override
    public void onPositiveClick(DialogFragment dialog, String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permName}, Constants.REQUEST_PERMISSION_FINE_LOCATION);
    }

    @Override
    public void onNegativeClick(DialogFragment dialgo) {

    }
}
