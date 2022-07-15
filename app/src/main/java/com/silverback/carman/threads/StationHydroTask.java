package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.StationViewModel;

import java.util.List;

public class StationHydroTask extends ThreadTask implements StationHydroRunnable.HydroStationCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationHydroTask.class);

    static final int HYDRO_STATE_SUCCEED = 1;
    static final int HYDRO_STATE_FAIL = -1;

    public Context context;
    public Runnable hydroStationListRunnable;
    public StationViewModel model;
    public Location location;

    public StationHydroTask(Context context, StationViewModel model, Location location) {
        hydroStationListRunnable = new StationHydroRunnable(context, this);
        this.context = context;
        this.model = model;
        this.location = location;
    }

    public Runnable getHydroListRunnable() {
        return hydroStationListRunnable;
    }

    @Override
    public void setHydroStationThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public Location getHydroLocation() {
        return location;
    }

    @Override
    public void setFirebaseHydroList(List<StationHydroRunnable.HydroStationObj> hydroList) {
        if(hydroList.size() > 0) {
            log.i("post hydro value");
            model.getHydroStationList().postValue(hydroList);
        }
    }

    @Override
    public void notifyException(String msg) {
        model.getHydroExceptionMessage().postValue(msg);
    }

    @Override
    public void handleTaskState(int state) {
        int outstate = -1;
        switch (state) {
            case HYDRO_STATE_SUCCEED:
                break;
            case HYDRO_STATE_FAIL:
                break;
        }
    }
}
