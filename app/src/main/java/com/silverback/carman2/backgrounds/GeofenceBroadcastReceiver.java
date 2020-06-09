package com.silverback.carman2.backgrounds;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * This BroadcastReceiver notices that the user has entered or exited a geofence, on which Location
 * Services has invoked the intent contained in the PendingIntent that the user included in the request
 * to add geofences. The receiver obtains the geofencing event from the intent that determine the type
 * of Geofence transitions and which of the defined geofences was triggered.
 *
 * Accroding to an action set in the intent, it handles either the general notification or the snooze
 * notification.
 *
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceBroadcastReceiver.class);

    // Objects
    private Context context;
    private NotificationManagerCompat notiManager;
    private long geoTime;

    @Override
    public void onReceive(Context context, Intent intent) {
        log.i("Geofence broadcasting: %s", intent);
        this.context = context;
        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(context);
        notiManager = NotificationManagerCompat.from(context);
        String action = intent.getAction();
        if(action == null) return;
        log.i("action: %s", intent.getAction());

        // Fork the procedure according to Intent.getAction() which should be either Geofencing
        // or Snoozing.
        switch(action) {
            case Constants.NOTI_GEOFENCE:
                GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
                if(geofencingEvent.hasError()) {
                    String errMsg = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
                    Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Get the transition type and send notificatoin as far as Geofence.Geofence_TRANSITION_
                // ENTER is concerned.
                int geofenceTransition = geofencingEvent.getGeofenceTransition();
                if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Location geofenceLocation = geofencingEvent.getTriggeringLocation();
                    List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
                    Location favLocation = new Location("@null");
                    log.i("GeofenceTransitionDetails: %s", triggeringGeofences);

                    // Retrieve all the favorite list. What if multiple providers are closely located
                    // within the radius?
                    List<FavoriteProviderEntity> entities = mDB.favoriteModel().loadAllFavoriteProvider();
                    geoTime = System.currentTimeMillis();

                    // Compare the location in the db with the geofencing location, then get a provider
                    // located within the preset radius.
                    for (FavoriteProviderEntity entity : entities) {
                        favLocation.setLongitude(entity.longitude);
                        favLocation.setLatitude(entity.latitude);

                        if (geofenceLocation.distanceTo(favLocation) < Constants.GEOFENCE_RADIUS) {
                            final int notiId = createID();
                            final String name = entity.providerName;
                            final String id = entity.providerId;
                            final String addrs = entity.address;
                            final int category = entity.category;
                            log.i("FavoriteProviderEntity: %s, %s, %s, %s, %s", notiId, name, id, addrs, category);

                            createNotification(notiId, id, name, addrs, category);
                            break;
                        }
                    }
                }

                break;

            case Constants.NOTI_SNOOZE:
                log.i("Snooze noti");
                String providerId = intent.getStringExtra(Constants.GEO_ID);
                String providerName = intent.getStringExtra(Constants.GEO_NAME);
                String providerAddrs = intent.getStringExtra(Constants.GEO_ADDRS);
                int category = intent.getIntExtra(Constants.GEO_CATEGORY, -1);
                int snoozeNotiId = intent.getIntExtra(Constants.NOTI_ID, -1);
                geoTime = intent.getLongExtra(Constants.GEO_TIME, -1);

                createNotification(snoozeNotiId, providerId, providerName, providerAddrs, category);
                //Notification notiSnooze = createNotification(notiId++, providerName, category);
                //notiManager.notify(notiId++, notiSnooze);
                break;
        }
    }


    private void createNotification(int notiId, String providerId, String name, String addrs, int category) {
        // Make the notification title and bigText(contentText and extendedText).
        String extendedText = null;
        String visitingTime = BaseActivity.formatMilliseconds(context.getString(R.string.date_format_6), geoTime);
        String contentText = String.format("%s\n%s", visitingTime, addrs);

        switch(category) {
            case Constants.GAS:
                extendedText = context.getResources().getString(R.string.noti_geofence_content_gas);
                break;

            case Constants.SVC: // car center
                extendedText = context.getResources().getString(R.string.noti_geofence_content_svc);
                break;

            default: break;
        }

        // Create PendingIntents for setContentIntent and addAction(Snooze)
        PendingIntent resultPendingIntent = createResultPendingIntent(notiId, providerId, name, category);
        PendingIntent snoozePendingIntent = createSnoozePendingIntent(notiId, providerId, name, addrs, category);
        int icon = (category == Constants.GAS)? R.drawable.ic_gas_station : R.drawable.ic_service_center;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, Constants.CHANNEL_ID);
        mBuilder.setSmallIcon(icon)
                .setShowWhen(true)
                .setContentTitle(name)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText + "\n\n" + extendedText))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7 and below instead of the channel
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                // addAction(drawable, charsequence, pendingintent) is deprecated as of Android 7.
                // The icon is not available any more but it should be provided for the under 7.
                .addAction(R.drawable.ic_notification_snooze, "Snooze", snoozePendingIntent)
                .build();

        // Set an vibrator to the notification by the build version
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = createNotificationChannel();
            if(channel != null) channel.setVibrationPattern(new long[]{0, 500, 500, 500, 500, 500});
        } else mBuilder.setVibrate(new long[]{0, 500, 500, 500, 500, 500});

        Notification notification = mBuilder.build();

        // With the Noti tag, NotificationManager.cancel(id) does not work.
        //notiManager.notify(tag, notiId, notification);
        notiManager.notify(notiId, notification);
    }

    // Create PendingIntent to make ExpenseActivity started when pressing the notification by creating
    // TaskStackBuilder.
    private PendingIntent createResultPendingIntent(int notiId, String providerId, String name, int category) {
        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(context, ExpenseActivity.class);
        //no idea how it works.
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction(Constants.NOTI_GEOFENCE);
        resultIntent.putExtra(Constants.GEO_ID, providerId);
        resultIntent.putExtra(Constants.GEO_CATEGORY, category);
        resultIntent.putExtra(Constants.GEO_NAME, name);
        resultIntent.putExtra(Constants.GEO_TIME, geoTime);

        // Create the TaskStackBuilder and add the intent, which inflates the back stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        //stackBuilder.addParentStack(MainActivity.class);
        //stackBuilder.addNextIntent(resultIntent);

        // More research on what this works for.
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);)


        // Get the PendingIntent containing the entire back stack
        return stackBuilder.getPendingIntent(notiId, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    // Create PendingIntent which is used in SnoozeBroadcastReceiver when pressing "Snooze".
    private PendingIntent createSnoozePendingIntent(
            int notiId, String providerId, String name, String addrs, int category) {

        Intent snoozeIntent = new Intent(context, SnoozeBroadcastReceiver.class);
        snoozeIntent.setAction(Constants.NOTI_SNOOZE);
        snoozeIntent.putExtra(Constants.GEO_ID, providerId);
        snoozeIntent.putExtra(Constants.GEO_NAME, name);
        snoozeIntent.putExtra(Constants.GEO_CATEGORY, category);
        snoozeIntent.putExtra(Constants.GEO_TIME, geoTime);
        snoozeIntent.putExtra(Constants.GEO_ADDRS, addrs);
        snoozeIntent.putExtra(Constants.NOTI_ID,  notiId);

        return PendingIntent.getBroadcast(context, notiId, snoozeIntent, 0);
    }

    // Create a unique notification id.
    private int createID() {
        Calendar calendar = Calendar.getInstance();
        return Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.getDefault()).format(calendar.getTime()));

    }

    // Create Notification Channel only for Android 8+
    private NotificationChannel createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because the NotificationChannel class
        // is newly introduced, not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.noti_ch_name);
            String description = context.getString(R.string.noti_ch_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID, name, importance);
            channel.setDescription(description);

            notiManager.createNotificationChannel(channel);
            /*
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);
            */
            return channel;
        }

        return null;
    }

    // Check if Carman is running in the foreground.
    private boolean isAppOnForeground() {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);

        if(activityManager == null) return false;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if(appProcesses == null) return false;

        final String packageName = context.getApplicationContext().getPackageName();
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
