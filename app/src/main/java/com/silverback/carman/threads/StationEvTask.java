package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;
import android.os.Build;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.StationViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StationEvTask extends ThreadTask implements StationEvRunnable.ElecStationCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationEvTask.class);
    private static final String regexEvName = "\\d*\\([\\w\\s]*\\)";

    static final int EV_TASK_SUCCESS = 1;
    static final int EV_TASK_FAIL = -1;

    private final Location location;
    private final Context context;
    private final StationViewModel viewModel;
    private Thread currentThread;

    private final List<StationEvRunnable.Item> evStationList;
    private int page = 1;

    public StationEvTask(Context context, StationViewModel viewModel, Location location){
        this.context = context;
        this.location = location;
        this.viewModel = viewModel;

        evStationList = new ArrayList<>();
    }


    public Runnable getElecStationListRunnable(int queryPage) {
        return new StationEvRunnable(context, queryPage, this);
    }

    public void recycle(){
        log.i("recycle StationEvTask");
    }

    @Override
    public void setEvStationTaskThread(Thread thread) {
        log.i("ev thread: %s", thread);
        this.currentThread = thread;
        setCurrentThread(thread);
    }

    @Override
    public Location getEvStationLocation() {
        return location;
    }

    @Override
    public void setEvStationList(List<StationEvRunnable.Item> evList) {
        if(evList != null && evList.size() > 0) evStationList.addAll(evList);
        // Sort EvList in the distance-descending order
        if(page == 5){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Collections.sort(evStationList, Comparator.comparingInt(t -> (int) t.getDistance()));
            else Collections.sort(evStationList, (t1, t2) ->
                    Integer.compare((int) t1.getDistance(), (int) t2.getDistance()));
            log.i("evStationList: %s", evStationList.size());
            viewModel.getEvStationList().postValue(evStationList);
            return;
        }

        page++;
    }

    @Override
    public void notifyEvStationError(Exception e) {
        viewModel.getExceptionMessage().postValue(String.valueOf(e.getLocalizedMessage()));
        page++;
    }

    @Override
    public void handleTaskState(int state) {
        int outstate = 0;
        switch(state) {
            case EV_TASK_SUCCESS:
                outstate = sThreadManager.TASK_COMPLETE;
                break;
            case EV_TASK_FAIL:
                outstate = sThreadManager.TASK_FAIL;
                break;
            default:break;
        }

        sThreadManager.handleState(this, outstate);
    }

    public Thread getCurrentThread() {
        return currentThread;
    }

}
