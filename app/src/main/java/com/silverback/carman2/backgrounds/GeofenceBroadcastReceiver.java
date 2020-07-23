package com.silverback.carman2.backgrounds;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.PreferenceManager;

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
 * Receiving geofence transition events from Location Serivces in the form of an intent containing
 * the transition type and geofence id that has trigeered the transition, start JobIntentService
 * to create a notification as the output.
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceBroadcastReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        log.i("Geofence broadcasting: %s", intent);

        // Trigger the geofence if the notification is set to true in the setting.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isGeofence = settings.getBoolean(Constants.NOTIFICATION_GEOFENCE, true);
        log.i("isGeofence: %s", isGeofence);
        if(isGeofence) GeofenceJobIntentService.enqueueWork(context, intent);
    }
}
