package com.silverback.carman.threads;

import android.location.Location;

public class ElecStationListTask extends ThreadTask implements ElecStationListRunnable.ElecStationCallback {

    private final Runnable elecStationListRunnable;
    private final Location location;
    private Thread currentThread;

    public ElecStationListTask(Location location) {
        elecStationListRunnable = new ElecStationListRunnable(this);
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
