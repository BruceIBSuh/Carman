package com.silverback.carman2;

import android.Manifest;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;


import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    protected static SharedPreferences mSettings;
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

    //Create Singleton SharedPreferences for initial default variables.
    //Be mindful not to be confused with another SharedPreferences that is for setting variables using
    //PreferenceMaanger.getDefaultPreferences().
    /*
    public static SharedPreferences getSharedPreferenceInstance(Context context) {
        if(mSettings == null) {
            mSettings = PreferenceManager
                    .getDefaultSharedPreferences(context.getApplicationContext());
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
        List<String> distCodeList = new ArrayList<>();

        if(jsonString == null) return null;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for(int i = 0; i < jsonArray.length(); i++) distCodeList.add(jsonArray.get(i).toString());
            return distCodeList;
        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

        return null;
    }


    // Formats date and time with milliseconds
    public static String formatMilliseconds(String format, long milliseconds) {
        //Date date = new Date(milliseconds);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(milliseconds);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(calendar.getTime());

    }

    // Check the time interval between the current time and the last update time saved in
    // SharedPreferences under the name of "Constants.OPINET_UPDATE_TIME", and the extra boolean
    // value passed from SettingsActivity that is true when the DistCode has changed,
    // then decide whether to newly update for the opinet price list or not
    // Big Bug here
    protected boolean checkUpdateOpinet() {
        long currentTime = System.currentTimeMillis();
        long lastUpdate = mSettings.getLong(Constants.OPINET_LAST_UPDATE, 0L);
        return(currentTime - lastUpdate) > Constants.OPINET_UPDATE_INTERVAL;
    }

    /**
     * Permission Check: Location, Read External Storage
     *
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


}
