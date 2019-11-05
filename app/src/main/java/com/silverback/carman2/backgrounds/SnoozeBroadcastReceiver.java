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
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import java.util.concurrent.TimeUnit;

public class SnoozeBroadcastReceiver extends BroadcastReceiver {

    private static final LoggingHelper log = LoggingHelperFactory.create(SnoozeBroadcastReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        String providerName = intent.getStringExtra("providerName");
        int category = intent.getIntExtra("category", -1);
        int notiId = intent.getIntExtra("notiId", -1);
        long geoTime = intent.getLongExtra("geoTime", -1);
        log.i("Extras in Receiver: %s, %s, %s, %s", providerName, category, notiId, geoTime);

        Intent geoIntent = new Intent(context, GeofenceTransitionService.class);
        geoIntent.setAction(Constants.NOTI_SNOOZE);
        geoIntent.putExtra("providerName", providerName);
        geoIntent.putExtra("category", category);
        geoIntent.putExtra("geoTime", geoTime);
        geoIntent.putExtra("notiId", notiId);

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, geoIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create WorkRequest
        Constraints constraints = new Constraints.Builder()
                .build();

        OneTimeWorkRequest snoozeWorkRequest = new OneTimeWorkRequest.Builder(NotificationSnoozeWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(10, TimeUnit.SECONDS)
                .addTag("snooze")
                .build();

        WorkManager.getInstance(context).enqueue(snoozeWorkRequest);




        /*
        long delay = 1000 * 60;

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pendingIntent);
        }
         */

        NotificationManagerCompat.from(context).cancelAll();
    }


    class NotificationSnoozeWorker extends Worker {

        public NotificationSnoozeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }


        @NonNull
        @Override
        public Result doWork() {

            log.i("WorkManager");

            return Result.success();
        }
    }
}
