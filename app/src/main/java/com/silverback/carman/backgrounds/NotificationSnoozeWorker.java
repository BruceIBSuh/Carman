package com.silverback.carman.backgrounds;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.silverback.carman.utils.Constants;

/*
 * The worker class is managed by WorkerManager defined in SnoozeBroadcastReceiver which manages
 * the worker to be delayed for a fixed time and send an broadcast with an intent containing geofence
 * data. When GeofenceBroadcastReceiver once receives an intent, it starts the GeofenceJobIntentService
 * with getting NOTI_SNOOZE action.
 */
public class NotificationSnoozeWorker extends Worker {

    //private static final LoggingHelper log = LoggingHelperFactory.create(NotificationSnoozeWorker.class);

    // Objects
    private Context context;

    public NotificationSnoozeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        // Get the data which have been set in OneTimeWorkRequest defined in SnoozeBoradcastReceiver.
        String providerName = getInputData().getString(Constants.GEO_NAME);
        String providerId = getInputData().getString(Constants.GEO_ID);
        String providerAddrs = getInputData().getString(Constants.GEO_ADDRS);
        int category = getInputData().getInt(Constants.GEO_CATEGORY, -1);
        long geoTime = getInputData().getLong(Constants.GEO_TIME, 0L);
        int notiId = getInputData().getInt(Constants.NOTI_ID, -1);

        // Put the data to the intent and send out to GeofenceBroadcastReceiver for displaying the
        // notification.
        Intent geoIntent = new Intent(context, GeofenceBroadcastReceiver.class);
        geoIntent.setAction(Constants.NOTI_SNOOZE);
        geoIntent.putExtra(Constants.GEO_ID, providerId);
        geoIntent.putExtra(Constants.GEO_NAME, providerName);
        geoIntent.putExtra(Constants.GEO_ADDRS, providerAddrs);
        geoIntent.putExtra(Constants.GEO_CATEGORY, category);
        geoIntent.putExtra(Constants.GEO_TIME, geoTime);
        geoIntent.putExtra(Constants.NOTI_ID, notiId);

        //PendingIntent.getBroadcast(context, notiId, geoIntent, PendingIntent.FLAG_ONE_SHOT);
        context.sendBroadcast(geoIntent);

        return Result.success();
    }
}
