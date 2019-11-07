package com.silverback.carman2.backgrounds;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

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

        String providerId = intent.getStringExtra(Constants.GEO_ID);
        String providerName = intent.getStringExtra(Constants.GEO_NAME);
        int category = intent.getIntExtra(Constants.GEO_CATEGORY, -1);
        int notiId = intent.getIntExtra(Constants.NOTI_ID, -1);
        long geoTime = intent.getLongExtra(Constants.GEO_TIME, -1);
        log.i("Extras in Receiver: %s, %s, %s, %s", providerName, category, notiId, geoTime);

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


        // Create WorkRequest
        Constraints constraints = new Constraints.Builder().build();
        Data geoData = new Data.Builder()
                .putString(Constants.GEO_ID, providerId)
                .putString(Constants.GEO_NAME, providerName)
                .putInt(Constants.GEO_CATEGORY, category)
                .putLong(Constants.GEO_TIME, geoTime)
                .build();

        OneTimeWorkRequest snoozeWorkRequest = new OneTimeWorkRequest.Builder(NotificationSnoozeWorker.class)
                .setConstraints(constraints)
                .setInputData(geoData)
                //.setInitialDelay(10, TimeUnit.SECONDS)
                .addTag(String.valueOf(notiId))
                .build();

        WorkManager.getInstance(context).enqueue(snoozeWorkRequest);
    }

}
