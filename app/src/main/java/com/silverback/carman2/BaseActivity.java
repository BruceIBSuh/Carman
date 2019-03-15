package com.silverback.carman2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.silverback.carman2.models.Constants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    // Constants
    private static final String LOG_TAG = "BaseActivity";

    // Objects
    protected static SharedPreferences mSettings;
    // Fields

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(mSettings == null) {
            mSettings = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
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

        //SharedPreferences mSettings = getSharedPreferenceInstance(this);
        String[] defaultParams = new String[3];
        defaultParams[0] = mSettings.getString(Constants.FUEL, "B027");
        defaultParams[1] = mSettings.getString(Constants.RADIUS, "2500");
        defaultParams[2] = mSettings.getString(Constants.ORDER, "2");

        return defaultParams;
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

}
