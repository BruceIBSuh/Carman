package com.silverback.carman.backgrounds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

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
