package com.silverback.carman2.backgrounds;

import android.content.Context;
import android.content.Intent;

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
        //AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        // MAKE THE SNOOZE DURATION SET IN SETTINGPREFERENCEACTIVITY WITH ANDROID N OR BELOW.(NOT CODED YET)
        // ANDROID O AND HIGHER, THE DURATION IS SET BY EMBEDDED FUNCTION.
        //final long delay = Constants.SNOOZE_DURATION;

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

        /*
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pendingIntent);
        }
         */

        return Result.success();
    }
}
