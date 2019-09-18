package com.silverback.carman2.threads;

import android.content.Context;
import android.location.Location;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.LocationViewModel;

public class GeocoderReverseTask extends ThreadTask implements GeocoderReverseRunnable.GeocoderMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeocoderReverseTask.class);

    // Objects
    private Context context;
    private Runnable mGeocoderReverseRunnable;
    private LocationViewModel model;
    private Location location;

    GeocoderReverseTask(Context context) {
        this.context = context;
    }

    void initGeocoderReverseTask(LocationViewModel model, Location location) {
        mGeocoderReverseRunnable = new GeocoderReverseRunnable(context, this);
        this.model = model;
        this.location = location;
    }

    Runnable getGeocoderRunnable() {
        return mGeocoderReverseRunnable;
    }

    void recycle() {}

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void setGeocoderThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setAddress(String address) {
        model.getAddress().postValue(address);
    }

    @Override
    public void handleGeocoderReverseTask(int state) {
        int outstate = -1;
        switch(state) {
            case GeocoderReverseRunnable.GEOCODER_REVERSE_SUCCESS:
                outstate = ThreadManager.GEOCODER_REVERSE_TASK_COMPLETED;
                break;

            case GeocoderReverseRunnable.GEOCODER_REVERSE_FAIL:
                outstate = ThreadManager.GEOCODER_REVERSE_TASK_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }


}
