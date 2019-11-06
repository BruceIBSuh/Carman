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

import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */

public class GeofenceTransitionService extends IntentService {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceTransitionService.class);

    // Constants
    final int SUMMARY_ID = 0;
    private final String GROUP_KEY_NOTIFICATION = "com.silverback.carman2.groupnoti";

    // Objects
    private NotificationManagerCompat notiManager;
    private SparseArray<Notification> sparseNotiArray;
    private long geoTime;
    private String providerName;
    private int category;
    private int notiId;


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
                sparseNotiArray = new SparseArray<>();
                geoTime = System.currentTimeMillis();
                notiId = 1;

                for(FavoriteProviderEntity entity : entities) {
                    favLocation.setLongitude(entity.longitude);
                    favLocation.setLatitude(entity.latitude);

                    if(geofenceLocation.distanceTo(favLocation) < Constants.GEOFENCE_RADIUS) {
                        providerName = entity.providerName;
                        category = entity.category;
                        createNotification(notiId++, providerName, category);
                        //sparseNotiArray.put(notiId++, createNotification(notiId++, providerName, category));
                    }
                }


                break;

            case Constants.NOTI_SNOOZE:
                providerName = intent.getStringExtra("providerName");
                category = intent.getIntExtra("category", -1);
                geoTime = intent.getLongExtra("geoTime", -1);
                //int id = intent.getIntExtra("notiId", -1);

                createNotification(notiId++, providerName, category);
                //Notification notiSnooze = createNotification(notiId++, providerName, category);
                //notiManager.notify(notiId++, notiSnooze);
                break;
        }

        // Check if the notification is in a group or single based upon how many providers are located
        // within the geofence radius, then handle to send notification in a diffenrent way by
        // the build version.
        /*
        if(sparseNotiArray == null) return;
        if(sparseNotiArray.size() > 1) {
            //if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Notification summaryNoti = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Multi Providers")
                        .setContentText("Multi providers are located within the radius")
                        .setGroup(GROUP_KEY_NOTIFICATION)
                        .setGroupSummary(true)
                        .build();

                for(int i = 0; i < sparseNotiArray.size(); i++) {
                    notiManager.notify(sparseNotiArray.keyAt(i), sparseNotiArray.valueAt(i));
                }

                notiManager.notify(SUMMARY_ID, summaryNoti);


            } else {

                    notiManager.notify(sparseNotiArray.keyAt(i), sparseNotiArray.valueAt(i));

            }

        } else {
            notiManager.notify(sparseNotiArray.keyAt(0), sparseNotiArray.valueAt(0));
        }
        */



    }

    private void createNotification(int notiId, String name, int category) {

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
        PendingIntent resultPendingIntent = createResultPendingIntent();
        PendingIntent snoozePendingIntent = createSnoozePendingIntent(notiId, category);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(true)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText + "\n" + extendedText))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7 and below instead of the channel
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                //.setGroup(GROUP_KEY_NOTIFICATION)
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
        //String tag = (category == Constants.GAS)? "noti_gas" : "noti_svc";
        //notiManager.notify(tag, notiId, notification);
        notiManager.notify(notiId, notification);

    }



    private PendingIntent createSnoozePendingIntent(int notiId, int category) {
        Intent snoozeIntent = new Intent(this, SnoozeBroadcastReceiver.class);
        snoozeIntent.setAction(Constants.NOTI_SNOOZE);
        snoozeIntent.putExtra("providerName", providerName);
        snoozeIntent.putExtra("category", category);
        snoozeIntent.putExtra("geoTime", geoTime);
        snoozeIntent.putExtra("notiId",  notiId);

        return PendingIntent.getBroadcast(this, notiId, snoozeIntent, 0);
    }


    private PendingIntent createResultPendingIntent() {
    //private PendingIntent createResultPendingIntent(final Class<? extends Activity> cls) {
        // Create an Intent for the activity you want to start
        //Intent resultIntent = new Intent(this, cls);
        Intent resultIntent = new Intent(this, ExpenseActivity.class);
        resultIntent.putExtra(Constants.GEO_CATEGORY, category);
        resultIntent.putExtra(Constants.GEO_NAME, providerName);

        // Create the TaskStackBuilder and add the intent, which inflates the back stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        //stackBuilder.addParentStack(MainActivity.class);
        //stackBuilder.addNextIntent(resultIntent);



        // Get the PendingIntent containing the entire back stack
        return stackBuilder.getPendingIntent(notiId, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    // Create Notification Channel only for Android 8+
    private NotificationChannel createNotificationChannel() {
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
