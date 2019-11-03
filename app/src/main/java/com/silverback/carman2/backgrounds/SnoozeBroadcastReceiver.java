package com.silverback.carman2.backgrounds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

public class SnoozeBroadcastReceiver extends BroadcastReceiver {

    private static final LoggingHelper log = LoggingHelperFactory.create(SnoozeBroadcastReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        String providerName = intent.getStringExtra("providerName");
        int category = intent.getIntExtra("category", -10);
        int notiId = intent.getIntExtra("notiId", -100);
        log.i("Extras: %s, %s, %s", providerName, category, notiId);

        Intent geoIntent = new Intent(context, GeofenceTransitionService.class);
        geoIntent.setAction(Constants.NOTI_SNOOZE);
        geoIntent.putExtra("providerName", providerName);
        geoIntent.putExtra("category", category);
        geoIntent.putExtra("notiId", notiId);

        /*
        try {
            Thread.sleep(10000);
        } catch(InterruptedException e) {
            log.e("InterruptedException: %s", e.getMessage());
        }
         */

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            log.d("startForegroundService");
            context.startForegroundService(geoIntent);
        } else {
            log.d("startService");
            context.startService(geoIntent);
        }

    }
}
