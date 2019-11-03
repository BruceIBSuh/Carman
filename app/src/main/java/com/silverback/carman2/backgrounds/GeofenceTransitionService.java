package com.silverback.carman2.backgrounds;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IntentService;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.MainActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.FavoriteGeofenceHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class GeofenceTransitionService extends IntentService {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceTransitionService.class);

    // Objects
    private NotificationManagerCompat notificationManager;

    // Fields
    private long geoTime;
    private String providerName;
    private int category;
    private int notificationId;


    public GeofenceTransitionService() {
        super("GeofenceTransitionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(this);
        notificationManager = NotificationManagerCompat.from(this);
        geoTime = System.currentTimeMillis();

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
                List<FavoriteProviderEntity> entities = mDB.favoriteModel().loadAllFavoriteProvider();
                for(FavoriteProviderEntity entity : entities) {

                    favLocation.setLongitude(entity.longitude);
                    favLocation.setLatitude(entity.latitude);

                    if(geofenceLocation.distanceTo(favLocation) < Constants.GEOFENCE_RADIUS) {
                        providerName = entity.providerName;
                        category = entity.category;
                        break;
                    }
                }


                log.i("Noti ID: %s", notificationId);
                sendNotification(providerName, category);
                break;

            case Constants.NOTI_SNOOZE:
                log.i("Snooze Intent");
                providerName = intent.getStringExtra("providerName");
                category = intent.getIntExtra("category", 0);
                int id = intent.getIntExtra("notiId", -1);
                log.i("snooze extras: %s, %s, %s", providerName, notificationId, id);


                /*
                try {
                    Thread.sleep(10000);
                } catch(InterruptedException e) {
                    log.e("sleep() interrupted", e.getMessage());
                }
                 */
                sendNotification(providerName, category);
                break;

            case Constants.NOTI_DISMISS:
                log.i("Dismiss clicked");
                log.i("noti id: %s", notificationId);
                notificationManager.cancel(notificationId);
                break;
        }

        //sendNotification(category);
    }

    private void sendNotification(String name, int category) {
        notificationId = createID();
        String title = null;
        String extendedText = null;

        String strTime = BaseActivity.formatMilliseconds(getString(R.string.date_format_6), geoTime);
        String contentText = String.format("%s %s", name, strTime);

        switch(category) {
            case Constants.GAS: // gas station
                title = getString(R.string.geofence_notification_gas);
                extendedText = getResources().getString(R.string.geofence_notification_gas_open);
                break;

            case Constants.SVC: // car center
                title = getString(R.string.geofence_notification_service);
                extendedText = getResources().getString(R.string.geofence_notification_service_open);
                break;

            default:
                break;
        }

        // Create the Snooze PendingIntent
        Intent snoozeIntent = new Intent(this, SnoozeBroadcastReceiver.class);
        snoozeIntent.setAction(Constants.NOTI_SNOOZE);

        snoozeIntent.putExtra("providerName", providerName);
        snoozeIntent.putExtra("category", 20);
        snoozeIntent.putExtra("notiId", 35353);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(this, 0, snoozeIntent, 0);


        Intent dismissIntent = new Intent(this, GeofenceTransitionService.class);
        dismissIntent.setAction(Constants.NOTI_DISMISS);
        log.i("Noti extra: %s", notificationId);
        //dismissIntent.putExtra("notificationId", notificationId);
        PendingIntent dismissPendingIntent = PendingIntent.getService(this, 0, dismissIntent, 0);


        // Create the PendingIntent to start ExpenseActivity and the relevant fragment according to
        // its extra.
        //PendingIntent resultPendingIntent = createResultPendingIntent(ExpenseActivity.class);
        PendingIntent resultPendingIntent = createResultPendingIntent();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(true)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText + "\n\n" + extendedText))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7 and below instead of the channel
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_snooze_foreground, "Snooze", snoozePendingIntent)
                .addAction(R.drawable.ic_snooze_foreground, "Dismiss", dismissPendingIntent);

        // Set an vibrator to the notification by the build version
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

    // Create PendingIntent which is to be sent to the param Activity with

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
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    // Create a unique notification id. Refactor considered!!
    private int createID() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        return Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.getDefault()).format(calendar.getTime()));

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
