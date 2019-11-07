package com.silverback.carman2.backgrounds;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

public class NotificationSnoozeWorker extends Worker {

    private static final LoggingHelper log = LoggingHelperFactory.create(NotificationSnoozeWorker.class);

    // Objects
    private Context context;

    public NotificationSnoozeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        /*
         * Refactor considered. WorkManager and AlarmManager put in togetehr should be OK?
         * More research is required.
         */
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        final long delay = 1000 * 60 * 5;

        String providerName = getInputData().getString(Constants.GEO_NAME);
        String providerId = getInputData().getString(Constants.GEO_ID);
        int category = getInputData().getInt(Constants.GEO_CATEGORY, -1);
        long geoTime = getInputData().getLong(Constants.GEO_TIME, 0L);
        int notiId = getInputData().getInt(Constants.NOTI_ID, -1);

        log.i("Worker input data: %s, %s, %s", providerName, category, geoTime);

        Intent geoIntent = new Intent(context, GeofenceTransitionService.class);
        geoIntent.setAction(Constants.NOTI_SNOOZE);
        geoIntent.putExtra(Constants.GEO_ID, providerId);
        geoIntent.putExtra(Constants.GEO_NAME, providerName);
        geoIntent.putExtra(Constants.GEO_CATEGORY, category);
        geoIntent.putExtra(Constants.GEO_TIME, geoTime);
        PendingIntent pendingIntent = PendingIntent.getService(context, notiId, geoIntent, PendingIntent.FLAG_ONE_SHOT);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pendingIntent);
        }

        return Result.success();
    }
}
