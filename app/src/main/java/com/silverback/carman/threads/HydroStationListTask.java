package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ExcelToJsonUtil;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.List;

public class HydroStationListTask extends ThreadTask implements HydroStationListRunnable.HydroStationCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(HydroStationListTask.class);

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

    }

    @Override
    public void setHydroList(List<ExcelToJsonUtil.HydroStationInfo> hydroList) {
        for(ExcelToJsonUtil.HydroStationInfo info : hydroList) {
            log.i("Hydro Info: %s, %s", info.getName(), info.getAddrs());
        }
    }


    @Override
    public Location getHydroLocation() {
        return location;
    }
}
