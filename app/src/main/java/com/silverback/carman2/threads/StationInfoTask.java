package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import static com.silverback.carman2.threads.StationInfoRunnable.DOWNLOAD_STN_INFO_COMPLTED;
import static com.silverback.carman2.threads.StationInfoRunnable.DOWNLOAD_STN_INFO_FAILED;

public class StationInfoTask extends ThreadTask implements StationInfoRunnable.StationInfoMethod {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationInfoTask.class);

    private Context context;
    private Opinet.GasStnParcelable stnParcelable;
    //private Opinet.GasStationInfo stnInfo;
    private Runnable stationInfoRunnable;

    private int index;

    public StationInfoTask() {
        super();
        stationInfoRunnable = new StationInfoRunnable(this);
    }

    void initDownloadInfoTask(ThreadManager threadManager, Opinet.GasStnParcelable stnParcelable) {
        sThreadManager = threadManager;
        this.stnParcelable = stnParcelable;
        //this.index = index;
    }

    Runnable getStationInfoRunnable() { return stationInfoRunnable; }

    @Override
    public void setTaskThread(Thread thread) {

    }

    @Override
    public int getIndex(){
        return index;
    }

    @Override
    public String getStationId() {
        return stnParcelable.getStnId();
    }

    @Override
    public void setStationInfo(Opinet.GasStationInfo stnInfo) {
        stnParcelable.setIsCarWash(stnInfo.getIsCarWash());
    }

    @Override
    public void handleTaskState(int state) {
        switch(state) {
            case DOWNLOAD_STN_INFO_COMPLTED:
                log.i("Station Info in Task: %s", stnParcelable.getIsCarWash());
                break;

            case DOWNLOAD_STN_INFO_FAILED:
                break;
        }
    }
}
