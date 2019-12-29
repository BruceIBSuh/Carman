package com.silverback.carman2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class BaseActivity extends AppCompatActivity {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BaseActivity.class);

    // Constants
    protected static final int REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1000;

    // Objects
    public static SharedPreferences mSettings;
    protected static DecimalFormat df;

    // Fields
    protected boolean hasLocationPermission;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Set screen to portrait as indicated with "android:screenOrientation="portrait" in Manifest.xml
        // android:screenOrientation is not allowed with Android O_MR1 +
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // SharedPreferences
        if(mSettings == null) {
            mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        }

    }

    /*
    public static SharedPreferences getSharedPreferenceInstance(Context context) {
        // SharedPreferences
        if(mSettings == null) {
            mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        }

        return mSettings;
    }

    */


    // DefaultParams: fuelCode, radius to locate, sorting radius
    protected final String[] getDefaultParams() {

        String[] defaultParams = new String[3];
        defaultParams[0] = mSettings.getString(Constants.FUEL, "B027");
        defaultParams[1] = mSettings.getString(Constants.RADIUS, "2500");
        defaultParams[2] = mSettings.getString(Constants.ORDER, "2");

        return defaultParams;
    }

    // Sido name, sig
    protected List<String> convJSONArrayToList() {

        String jsonString = mSettings.getString(Constants.DISTRICT, null);
        log.i("jsonString of District: %s", jsonString);
        if(jsonString == null) {
            JSONArray jsonArray = new JSONArray(Arrays.asList("서울", "종로구", "0101"));
            jsonString = jsonArray.toString();
        }

        List<String> distCodeList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for(int i = 0; i < jsonArray.length(); i++) distCodeList.add(jsonArray.get(i).toString());
            return distCodeList;
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

        return null;
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
            return date.getTime();
        } catch(ParseException e) {
            log.e("ParseException: %s", e.getMessage());
        }

        return -1;
    }

    // Check the time interval between the current time and the last update time saved in
    // SharedPreferences under the name of "Constants.OPINET_UPDATE_TIME", and the extra boolean
    // value passed from SettingsActivity that is true when the DistCode has changed,
    // then decide whether to newly update for the opinet price list or not
    // Big Bug here
    protected boolean checkUpdateOilPrice() {
        long currentTime = System.currentTimeMillis();
        long lastUpdate = mSettings.getLong(Constants.OPINET_LAST_UPDATE, 0L);
        return (currentTime - lastUpdate) > Constants.OPINET_UPDATE_INTERVAL;
    }

    /*
     * Permission Check: Location, Read External Storage
     * Location: ACCESS_FINE_LOCATION
     * External Storage: READ_EXTERNAL_STORAGE
     */
    public void checkPermissions() {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            log.d("permission still not granted");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSION_ACCESS_FINE_LOCATION);
            }

        } else hasLocationPermission = true;

    }

    // Abstract method which is invoked by ActivityCompat.requestPermissions()
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {

            case REQUEST_PERMISSION_ACCESS_FINE_LOCATION:
                // When clicking Accept button on the dialog
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    hasLocationPermission = true;
                    // When clicking Deny button on the dialog
                else {
                    hasLocationPermission = false;
                    // Check if the user checks "Never Ask Again". When checked,
                    // shouldShowRequestPermissionRationale returns false.
                    if(!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        log.i("Never Ask Again");
                    }
                }

                break;

            default:
                break;

        }
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

    /*
    public static Thread runCurrentTime(final Fragment fragment, String format, final TextView view){

        final Calendar calendar = Calendar.getInstance(Locale.getDefault());
        final SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());

        Thread timeThread = new Thread(){
            @Override
            public void run() {
                while (!isInterrupted()) {
                    fragment.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            calendar.setTimeInMillis(System.currentTimeMillis());
                            view.setText(sdf.format(calendar.getTime()));

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                log.e("InterruptedException: %s", e.getMessage());
                            }
                        }
                    });

                }
            }
        };

        return timeThread;

    }
    */

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

            log.i("context: %s", context);
            tabTitleList = Arrays.asList(getResources().getStringArray(R.array.tab_billboard_title));

        }

        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            log.i("Tab Title: %s", tabTitleList.get(i));
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

    public NotificationChannel createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence name = getString(R.string.noti_ch_name);
            String description = getString(R.string.noti_ch_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);

            return channel;
        }

        return null;
    }

    // Check a state of the network
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if(networkInfo != null) {
            int type = networkInfo.getSubtype();
        }

        return false;
    }


}
