package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.ServiceCenterViewModel;

public class ServiceCenterTask extends ThreadTask implements ServiceCenterRunnable.ServiceCenterMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceCenterTask.class);

    private final Runnable mServiceCenterRunnable;
    private Location mLocation;
    private ServiceCenterViewModel model;

    public ServiceCenterTask(Context context) {
        mServiceCenterRunnable = new ServiceCenterRunnable(context, this);
    }

    public void initServiceTask(ServiceCenterViewModel model, Location location) {
        this.model = model;
        mLocation = location;
    }

    Runnable getServiceCenterRunnable() {
        return mServiceCenterRunnable;
    }

    @Override
    public void setServiceCenterThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public Location getCurrentLocation() {
        return mLocation;
    }
}
