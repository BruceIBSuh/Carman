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

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.ExpenseActivity;
import com.silverback.carman2.NotificationActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class GeofenceTransitionService extends IntentService {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceTransitionService.class);
    private static final String REPLY_KEY_MILEAGE = "noti_key_reply_mileage";
    private static final String REPLY_KEY_PAY = "noti_key_replay_pay";
    private static final String REPLY_LABEL_MILEAGE = "MILEAGE";
    private static final String REPLY_LABEL_PAY = "PAYMENT";

    // Objects
    private Location geofenceLocation;
    private NotificationManagerCompat notificationManager;

    // Fields
    private String providerId, providerName;
    private long geofenceTime;
    private int category;


    public GeofenceTransitionService() {
        super("GeofenceTransitionService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        notificationManager = NotificationManagerCompat.from(this);

        providerId = intent.getStringExtra("providerId");
        providerName = intent.getStringExtra("providerName");
        category = intent.getIntExtra("category", Constants.GAS);
        log.i("Intent extras: %s, %s, %s", providerId, providerName, category);

        if(geofencingEvent.hasError()) {
            log.e("GeofencingEvent error occurred: %s", geofencingEvent.getErrorCode());
            return;
        }

        int geofencingTransition = geofencingEvent.getGeofenceTransition();
        switch(geofencingTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                break;

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                break;

            case Geofence.GEOFENCE_TRANSITION_DWELL:
                break;

            default:
                break;
        }



        geofenceLocation = geofencingEvent.getTriggeringLocation();
        sendNotification(category);

    }


    private void sendNotification(int category) {

        int notificationId = createID();
        geofenceTime = System.currentTimeMillis();
        String title = null;
        String extendedText = null;
        PendingIntent resultPendingIntent = createResultPendingIntent(ExpenseActivity.class);

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


        String strTime = BaseActivity.formatMilliseconds(getString(R.string.date_format_6), geofenceTime);
        String multiText = String.format("%s %s\n%s", providerName, strTime, extendedText);

        // Direct Reply action
        /*
        RemoteInput remoteInput = new RemoteInput.Builder("key_mileage_reply")
                .setLabel("current mileage")
                .build();
        Intent replyIntent = new Intent();
        PendingIntent replyPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_gas, "Mileage", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();
         */

        // Set up a special activity PendingIntent
        /*
        Intent notifyIntent = new Intent(this, NotificationActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        */

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(true)
                .setContentTitle(title)
                .setContentText(multiText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(extendedText))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7 and below instead of the channel
                .setContentIntent(resultPendingIntent)
                //.setContentIntent(notifyPendingIntent)
                .setAutoCancel(true);
                //.addAction(action);

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

    // Create PendingIntent which is to be sent to the param Activity with
    private PendingIntent createResultPendingIntent(final Class<? extends Activity> cls) {
        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, cls);

        resultIntent.putExtra(Constants.GEO_INTENT, true);
        resultIntent.putExtra(Constants.GEO_NAME, providerName);
        resultIntent.putExtra(Constants.GEO_ID, providerId);
        resultIntent.putExtra(Constants.GEO_TIME, geofenceTime);
        resultIntent.putExtra(Constants.GEO_LOCATION, geofenceLocation);

        // Create the TaskStackBuilder and add the intent, which inflates the back stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);

        // Get the PendingIntent containing the entire back stack
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    // Create a unique notification id. Refactor considered!!
    private int createID() {
        Calendar calendar = Calendar.getInstance();
        //calendar.setTimeInMillis(System.currentTimeMillis());

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
