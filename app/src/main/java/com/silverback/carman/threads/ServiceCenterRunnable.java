package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;
import android.os.Process;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ServiceCenterRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceCenterRunnable.class);

    private ServiceCenterMethods mTask;
    private Location currentLocation;

    public interface ServiceCenterMethods {
        void setServiceCenterThread(Thread thread);
        Location getCurrentLocation();
    }

    public ServiceCenterRunnable(Context context, ServiceCenterMethods task) {
        mTask = task;
    }


    @Override
    public void run() {
        mTask.setServiceCenterThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        currentLocation = mTask.getCurrentLocation();
        log.i("Current Location in Runnable: %s", currentLocation);

    }
}
