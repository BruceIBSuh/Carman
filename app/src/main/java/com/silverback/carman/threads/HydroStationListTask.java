package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ExcelToJsonUtil;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.List;

public class HydroStationListTask extends ThreadTask implements HydroStationListRunnable.HydroStationCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(HydroStationListTask.class);

    static final int HYDRO_STATE_SUCCEED = 1;
    static final int HYDRO_STATE_FAIL = -1;

    public Context context;
    public Runnable hydroStationListRunnable;
    public StationListViewModel model;
    public Location location;

    public HydroStationListTask(Context context, StationListViewModel model, Location location) {
        hydroStationListRunnable = new HydroStationListRunnable(context, this);
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

    /*
    @Override
    public void setHydroList(List<ExcelToJsonUtil.HydroStationObj> hydroList) {
        if(hydroList.size() > 0) model.getHydroStationList().postValue(hydroList);
    }

     */

    @Override
    public void setFirebaseHydroList(List<HydroStationListRunnable.HydroStationObj> hydroList) {
        log.i("hydroList: %s", hydroList.size());
        if(hydroList.size() > 0) model.getHydroStationList().postValue(hydroList);
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
