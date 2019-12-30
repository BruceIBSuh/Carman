package com.silverback.carman2.backgrounds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class GeofenceRebootReceiver extends BroadcastReceiver {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceRebootReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction() == null) return;
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equalsIgnoreCase(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {

            Intent geoIntent = new Intent(context, GeofenceResetService.class);

            // startService in the background is not allowed in API 26+ unless the application is
            // in the foreground except for some specific cases. JobScheduler or startForeground has
            // to be used instead.
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log.d("startForegroundService");
                context.startForegroundService(geoIntent);
            } else {
                log.d("startService");
                context.startService(geoIntent);
            }
        }

    }
}
