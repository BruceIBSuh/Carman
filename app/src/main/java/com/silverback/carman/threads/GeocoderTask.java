package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

import com.silverback.carman.viewmodels.LocationViewModel;

public class GeocoderTask extends ThreadTask implements GeocoderRunnable.GeocoderMethods {

    private Context context;
    private Runnable mGeocoderRunnable;
    private LocationViewModel model;
    private String addrs;

    GeocoderTask(Context context) {
        this.context = context;
        mGeocoderRunnable = new GeocoderRunnable(context, this);
    }

    void initGeocoderTask(LocationViewModel model, String addrs) {
        this.model = model;
        this.addrs = addrs;
    }

    Runnable getGeocoderRunnable() {
        return mGeocoderRunnable;
    }

    @Override
    public void setGeocoderThread(Thread thread) {
        setCurrentLocation(thread);
    }

    @Override
    public void setGeocoderLocation(Location location) {
        model.getGeocoderLocation().postValue(location);
    }

    @Override
    public String getGeocoderAddress() {
        return addrs;
    }
}
