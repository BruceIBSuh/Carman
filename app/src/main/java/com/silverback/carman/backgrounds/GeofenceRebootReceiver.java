package com.silverback.carman.backgrounds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class GeofenceRebootReceiver extends BroadcastReceiver {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceRebootReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction() == null) return;
        log.i("intent.getAction(): %s", intent.getAction());
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equalsIgnoreCase(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {

            // Create WorkRequest to put it in WorkManager.enqueue(). Constraints may be defined here
            // to indicate when or how the work runs.
            OneTimeWorkRequest resetGeofence = new OneTimeWorkRequest.Builder(GeofenceResetWorker.class).build();
            WorkManager.getInstance(context).enqueue(resetGeofence);
        }

    }
}
