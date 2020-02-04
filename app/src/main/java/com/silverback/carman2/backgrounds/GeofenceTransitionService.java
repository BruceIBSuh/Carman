package com.silverback.carman2.backgrounds;

import android.app.ActivityManager;
import android.app.IntentService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.util.SparseArray;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.GeofencingEvent;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


/**
 * This class subclasses IntentService for purposes of showing Notification when a location is within
 * the Geofence
 */

public class GeofenceTransitionService extends IntentService {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceTransitionService.class);

    // Objects
    private NotificationManagerCompat notiManager;
    private long geoTime;


    public GeofenceTransitionService() {
        super("GeofenceTransitionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(this);
        notiManager = NotificationManagerCompat.from(this);

        String action = intent.getAction();
        if(action == null) return;

        switch(action) {
            case Constants.NOTI_GEOFENCE:
                GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
                if(geofencingEvent.hasError()) return;

                //final int geofencingTransition = geofencingEvent.getGeofenceTransition();
                final Location geofenceLocation = geofencingEvent.getTriggeringLocation();
                Location favLocation = new Location("@null");


                // Retrieve all the favorite list.
                // What if multiple providers are closely located within the radius?
                List<FavoriteProviderEntity> entities = mDB.favoriteModel().loadAllFavoriteProvider();
                //sparseNotiArray = new SparseArray<>();
                geoTime = System.currentTimeMillis();
                //int notiId = 0;

                for(FavoriteProviderEntity entity : entities) {
                    favLocation.setLongitude(entity.longitude);
                    favLocation.setLatitude(entity.latitude);

                    if(geofenceLocation.distanceTo(favLocation) < Constants.GEOFENCE_RADIUS) {
                        final int notiId = createID();
                        final String name = entity.providerName;
                        final String id = entity.providerId;
                        final int category = entity.category;

                        createNotification(notiId, id, name, category);
                        //sparseNotiArray.put(notiId++, createNotification(notiId++, providerName, category));
                    }
                }


                break;

            case Constants.NOTI_SNOOZE:
                String providerId = intent.getStringExtra(Constants.GEO_ID);
                String providerName = intent.getStringExtra(Constants.GEO_NAME);
                int category = intent.getIntExtra(Constants.GEO_CATEGORY, -1);
                int snoozeNotiId = intent.getIntExtra(Constants.NOTI_ID, -1);
                geoTime = intent.getLongExtra(Constants.GEO_TIME, -1);

                createNotification(snoozeNotiId, providerId, providerName, category);
                //Notification notiSnooze = createNotification(notiId++, providerName, category);
                //notiManager.notify(notiId++, notiSnooze);
                break;
        }
    }

    private void createNotification(int notiId, String providerId, String name, int category) {
        log.i("notification ID: %s", notiId);
        String title = null;
        String extendedText = null;

        final String strTime = BaseActivity.formatMilliseconds(getString(R.string.date_format_6), geoTime);
        final String contentText = String.format("%s %s", name, strTime);
        log.i("Content Text: %s", contentText);

        switch(category) {
            case Constants.GAS: // gas station
                title = getString(R.string.noti_geofence_title_gas);
                extendedText = getResources().getString(R.string.noti_geofence_content_gas);
                break;

            case Constants.SVC: // car center
                title = getString(R.string.noti_geofence_title_svc);
                extendedText = getResources().getString(R.string.noti_geofence_content_svc);
                break;

            default:
                break;
        }

        // Create PendingIntents for setContentIntent and addAction(Snooze)
        PendingIntent resultPendingIntent = createResultPendingIntent(notiId, providerId, name, category);
        PendingIntent snoozePendingIntent = createSnoozePendingIntent(notiId, providerId, name, category);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(true)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText + "\n" + extendedText))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7 and below instead of the channel
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .addAction(-1, "Snooze", snoozePendingIntent)
                .build();


        // Set an vibrator to the notification by the build version
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = createNotificationChannel();
            if(channel != null) channel.setVibrationPattern(new long[]{0, 500, 500, 500});

        } else {
            mBuilder.setVibrate(new long[]{0, 500, 500, 500}); //Vibarate on receiving notification
        }

        Notification notification = mBuilder.build();

        // With the Noti tag, NotificationManager.cancel(id) does not work.
        //notiManager.notify(tag, notiId, notification);
        notiManager.notify(notiId, notification);

    }

    private PendingIntent createResultPendingIntent(int notiId, String providerId, String name, int category) {
        //private PendingIntent createResultPendingIntent(final Class<? extends Activity> cls) {
        // Create an Intent for the activity you want to start
        //Intent resultIntent = new Intent(this, cls);
        Intent resultIntent = new Intent(this, ExpenseActivity.class);
        resultIntent.setAction(Constants.NOTI_GEOFENCE);
        resultIntent.putExtra(Constants.GEO_ID, providerId);
        resultIntent.putExtra(Constants.GEO_CATEGORY, category);
        resultIntent.putExtra(Constants.GEO_NAME, name);
        resultIntent.putExtra(Constants.GEO_TIME, geoTime);

        // Create the TaskStackBuilder and add the intent, which inflates the back stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        //stackBuilder.addParentStack(MainActivity.class);
        //stackBuilder.addNextIntent(resultIntent);


        // Get the PendingIntent containing the entire back stack
        return stackBuilder.getPendingIntent(notiId, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    private PendingIntent createSnoozePendingIntent(int notiId, String providerId, String name, int category) {
        Intent snoozeIntent = new Intent(this, SnoozeBroadcastReceiver.class);
        snoozeIntent.setAction(Constants.NOTI_SNOOZE);
        snoozeIntent.putExtra(Constants.GEO_ID, providerId);
        snoozeIntent.putExtra(Constants.GEO_NAME, name);
        snoozeIntent.putExtra(Constants.GEO_CATEGORY, category);
        snoozeIntent.putExtra(Constants.GEO_TIME, geoTime);
        snoozeIntent.putExtra(Constants.NOTI_ID,  notiId);

        return PendingIntent.getBroadcast(this, notiId, snoozeIntent, 0);
    }

    // Create a unique notification id.
    private int createID() {
        Calendar calendar = Calendar.getInstance();
        //calendar.setTimeInMillis(System.currentTimeMillis());

        return Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.getDefault()).format(calendar.getTime()));

    }

    // Create Notification Channel only for Android 8+
    private NotificationChannel createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because the NotificationChannel class
        // is newly introduced, not in the support library
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
}
