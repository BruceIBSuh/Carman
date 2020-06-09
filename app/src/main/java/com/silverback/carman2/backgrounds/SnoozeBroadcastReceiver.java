package com.silverback.carman2.backgrounds;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SnoozeBroadcastReceiver extends BroadcastReceiver {

    private static final LoggingHelper log = LoggingHelperFactory.create(SnoozeBroadcastReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the snooze duration from sharedpreferences, the preference of which should be made
        // in SettingPreferenceActivity.
        int pause = PreferenceManager.getDefaultSharedPreferences(context).getInt("snooze", 5);

        String providerId = intent.getStringExtra(Constants.GEO_ID);
        String providerName = intent.getStringExtra(Constants.GEO_NAME);
        String providerAddrs = intent.getStringExtra(Constants.GEO_ADDRS);
        int category = intent.getIntExtra(Constants.GEO_CATEGORY, -1);
        int notiId = intent.getIntExtra(Constants.NOTI_ID, -1);
        long geoTime = intent.getLongExtra(Constants.GEO_TIME, -1);

        // Create Constraints and Data, both of which are applied to (OneTime)WorkRequest. The WorkRequest
        // is created with Worker class and WorkManager enqueue the request.
        Constraints constraints = new Constraints.Builder().build();
        Data geoData = new Data.Builder()
                .putString(Constants.GEO_ID, providerId)
                .putString(Constants.GEO_NAME, providerName)
                .putString(Constants.GEO_ADDRS, providerAddrs)
                .putInt(Constants.GEO_CATEGORY, category)
                .putLong(Constants.GEO_TIME, geoTime)
                .build();
        OneTimeWorkRequest snoozeWorkRequest = new OneTimeWorkRequest.Builder(NotificationSnoozeWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(pause, TimeUnit.MINUTES) //set the snooze duration by hours.
                .setInputData(geoData)
                .addTag(String.valueOf(notiId))
                .build();
        WorkManager.getInstance(context).enqueue(snoozeWorkRequest);

        // Cancel the notification with its id passed from the intent.
        NotificationManagerCompat.from(context).cancel(notiId);

        /*
        Intent geoIntent = new Intent(context, GeofenceTransitionService.class);
        geoIntent.setAction(Constants.NOTI_SNOOZE);
        geoIntent.putExtra("providerName", providerName);
        geoIntent.putExtra("category", category);
        geoIntent.putExtra("geoTime", geoTime);
        geoIntent.putExtra("notiId", notiId);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            log.i("getForegroundService");
            PendingIntent.getForegroundService(context, 100, geoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            log.i("getService");
            PendingIntent.getService(context, 100, geoIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        */
    }

}
