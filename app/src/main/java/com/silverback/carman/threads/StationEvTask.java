package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.List;

public class StationEvTask extends ThreadTask implements StationEvRunnable.ElecStationCallback {

    private final Runnable elecStationListRunnable;
    private final Location location;
    private Context context;
    private StationListViewModel viewModel;
    private Thread currentThread;

    public StationEvTask(Context context, StationListViewModel viewModel, Location location) {
        elecStationListRunnable = new StationEvRunnable(context, this);
        this.location = location;
        this.viewModel = viewModel;
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

    @Override
    public void setEvStationList(List<StationEvRunnable.Item> evList) {
        /*if(evList.size() > 0)*/ viewModel.getEvStationList().postValue(evList);
    }

    @Override
    public void notifyEvStationError(Exception e) {
        viewModel.getExceptionMessage().postValue(String.valueOf(e));
    }
}
