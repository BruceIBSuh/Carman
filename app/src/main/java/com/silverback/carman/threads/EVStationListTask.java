package com.silverback.carman.threads;

import android.location.Location;

public class EVStationListTask extends ThreadTask implements EVStationListRunnable.ElecStationCallback {

    private final Runnable elecStationListRunnable;
    private final Location location;
    private Thread currentThread;

    public EVStationListTask(Location location) {
        elecStationListRunnable = new EVStationListRunnable(this);
        this.location = location;
    }


    public Runnable getElecStationListRunnable() {
        return elecStationListRunnable;
    }

    @Override
    public void setElecStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public Location getElecStationLocation() {
        return location;
    }
}
