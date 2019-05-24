package com.silverback.carman2.services;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.DataProviderContract;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class GeofenceTransitionService extends IntentService {

    // Constants
    //private static final String LOG_TAG = "GeofenceTransition";
    private static final int GAS_STATION = 1;
    private static final int SERVICE_CENTER = 2;


    // Objects
    private NotificationManagerCompat notificationManager;
    private int geofenceCategory;
    private String geofenceName, geofenceId;
    private long geofenceTime;
    private Location geofenceLocation;

    private static final String[] projection = {
            DataProviderContract.FAVORITE_ID,
            DataProviderContract.FAVORITE_PROVIDER_CATEGORY,
            DataProviderContract.FAVORITE_PROVIDER_NAME,
            DataProviderContract.FAVORITE_PROVIDER_ID,
            DataProviderContract.FAVORITE_PROVIDER_LONGITUDE,
            DataProviderContract.FAVORITE_PROVIDER_LATITUDE
    };

    // Constructor
    public GeofenceTransitionService(){
        super("GeofenceTransitionService");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Location favoriteLocation = new Location("@null");
        // NotificationManagerCompat instantiated
        notificationManager = NotificationManagerCompat.from(this);
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if(geofencingEvent != null) {

            if(geofencingEvent.hasError()) {
                //Log.e(LOG_TAG, "GeofenceTransition Error: " + geofencingEvent.getErrorCode());
                return;
            }

            int geofenceTransition = geofencingEvent.getGeofenceTransition();
            geofenceLocation = geofencingEvent.getTriggeringLocation();

            // Convert the double type location data to integer type which includes 3 decimal places
            // by multiplying 1E3.
            /*
            int geoLong = (int)((geofenceLocation.getLongitude()) * 1E3);
            int geoLat = (int)((geofenceLocation.getLatitude()) * 1E3);
            Log.i(LOG_TAG, "Location from Geofence: " + geoLong + ", " + geoLat);
            */

            Cursor cursor = getApplicationContext().getContentResolver().query(
                    DataProviderContract.FAVORITE_TABLE_URI, projection, null, null, null);

            if(cursor == null) return;

            try {

                while (cursor.moveToNext()) {

                    // Convert double type location data queried from Favorite table to integer type
                    // with 3 decimal places as well to compare with triggeredLocation data.



                    /*
                    int lngFavorite = (int) (cursor.getDouble(4) * 1E3);
                    int latFavorite = (int) (cursor.getDouble(5) * 1E3);
                    Log.i(LOG_TAG, "Location from Favorite: " + lngFavorite + ", " + latFavorite);
                    if (lngFavorite == geoLong || latFavorite == geoLat) {
                        geofenceCategory = cursor.getInt(1);
                        geofenceName = cursor.getString(2);
                        geofenceId = cursor.getString(3);
                        Log.i(LOG_TAG, "Triggered Favorite: " + geofenceCategory + ", " + geofenceName + ", " + geofenceId);
                        break;
                    }
                    */

                    favoriteLocation.setLongitude(cursor.getDouble(4));
                    favoriteLocation.setLatitude(cursor.getDouble(5));

                    if(geofenceLocation.distanceTo(favoriteLocation) < Constants.GEOFENCE_FAVORITE_MATCH_RADIUS){
                        geofenceCategory = cursor.getInt(1);
                        geofenceName = cursor.getString(2);
                        geofenceId = cursor.getString(3);

                        //Log.i(LOG_TAG, "Triggered Favorite: " + geofenceCategory + ", " + geofenceName + ", " + geofenceId);
                        break;
                    }
                }

            } finally {

                switch (geofenceTransition) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        //Log.i(LOG_TAG, "GEOFENCE_TRANSITION_ENTER");
                        //if(!isAppOnForeground())
                        sendNotification();
                        break;
                    case Geofence.GEOFENCE_TRANSITION_DWELL:
                        //Log.i(LOG_TAG, "GEOFENCE_TRANSITION_DWELL");
                        if (!isAppOnForeground())
                            sendNotification();
                        break;
                    default:
                        break;
                }

                cursor.close();
            }



        }
    }

    private void sendNotification() {

        int notificationId = createID();
        geofenceTime = System.currentTimeMillis();
        String title = null;
        String extendedText = null;
        PendingIntent resultPendingIntent = null;

        switch(geofenceCategory) {
            case GAS_STATION: // gas station
                /*
                resultPendingIntent = createResultPendingIntent(GasManagerActivity.class);
                title = getString(R.string.geofence_notification_gas);
                extendedText = getResources().getString(R.string.geofence_notification_gas_open);
                */
                break;
            case SERVICE_CENTER: // car center
                /*
                resultPendingIntent = createResultPendingIntent(ServiceManagerActivity.class);
                title = getString(R.string.geofence_notification_service);
                extendedText = getResources().getString(R.string.geofence_notification_service_open);
                */
                break;
            default:
                break;
        }


        String strTime = BaseActivity.formatMilliseconds(getString(R.string.date_format_6), geofenceTime);
        String multiText = String.format("%s%1s%s%s%s", geofenceName, "", strTime, "\n\n", extendedText);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(true)
                .setContentTitle(title)
                .setContentText(multiText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(multiText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);
                /*
                .addAction(R.drawable.ic_snooze,
                        getString(R.string.geofence_notification_snooze), createSnoozePendingIntent());
                */

        // Set Vibrator to Notification by Build version
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = createNotificationChannel();
            if(channel != null) channel.setVibrationPattern(new long[]{0, 500, 500, 500});
        } else {
            mBuilder.setVibrate(new long[]{0, 500, 500, 500}); //Vibarate on receiving notification
        }

        // As far as Carman is not running in the foreground, start GasManagerActivity with Location
        // being passed to it.
        notificationManager.notify(Constants.CHANNEL_ID, notificationId, mBuilder.build());

    }


    // Check if Carman is running in the foreground.
    private boolean isAppOnForeground() {
        ActivityManager activityManager
                = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        if(activityManager == null) return false;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if(appProcesses == null) return false;

        final String packageName = getApplicationContext().getPackageName();
        // Check if Carman is running in the foreground by matching the package name with all the
        // package names running in the foreground.
        for(ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if(appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }

        return false;
    }


    // Create a unique notification id.
    private int createID() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        return Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.KOREA).format(calendar.getTime()));

    }

    // Create PendingIntent which is to be sent to the param Activity with
    private PendingIntent createResultPendingIntent(final Class<? extends Activity> cls) {

        //Log.d(LOG_TAG, "ResultPendingIntent");

        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, cls);

        resultIntent.putExtra(Constants.GEO_INTENT, true);
        resultIntent.putExtra(Constants.GEO_NAME, geofenceName);
        resultIntent.putExtra(Constants.GEO_ID, geofenceId);
        resultIntent.putExtra(Constants.GEO_TIME, geofenceTime);
        resultIntent.putExtra(Constants.GEO_LOCATION, geofenceLocation);

        // Create the TaskStackBuilder and add the intent, which inflates the back stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);

        // Get the PendingIntent containing the entire back stack
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    // Create Notification Channel only for Android 8+
    private NotificationChannel createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence name = getString(R.string.notification_ch_name);
            String description = getString(R.string.notification_ch_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);

            return channel;
        }

        return null;
    }

}
