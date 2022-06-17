package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;
import android.os.Build;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StationEvTask extends ThreadTask implements StationEvRunnable.ElecStationCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationEvTask.class);

    static final int EV_TASK_SUCCESS = 1;
    static final int EV_TASK_FAIL = -1;

    private final Runnable elecStationListRunnable;
    private final Location location;
    private Context context;
    private StationListViewModel viewModel;
    private Thread currentThread;

    private List<StationEvRunnable.Item> evStationList;
    private int index = 1;
    private int page;

    public StationEvTask(Context context, StationListViewModel viewModel, Location location) {
        elecStationListRunnable = new StationEvRunnable(context, this);
        this.location = location;
        this.viewModel = viewModel;

        evStationList = new ArrayList<>();
    }


    public void setCurrentPage(int page) {
        log.i("current page as param: %s", page);
        this.page = page;
    }

    public Runnable getElecStationListRunnable() {
        return elecStationListRunnable;
    }

    public void recycle(){
        log.i("recycle StationEvTask");
    }

    @Override
    public void setElecStationTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public Location getElecStationLocation() {
        return location;
    }
    /*
    @Override
    public int getCurrentPage() {
        return this.page;
    }

     */

    @Override
    public void setEvStationList(List<StationEvRunnable.Item> evList) {
        //if(index == 3)
        if(evList != null && evList.size() > 0) evStationList.addAll(evList);
        if(index == 5){
            log.i("total number: %s", evStationList.size());
            // Sort EvList in the distance-descending order
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Collections.sort(evStationList, Comparator.comparingInt(t -> (int) t.getDistance()));
            else Collections.sort(evStationList, (t1, t2) ->
                    Integer.compare((int) t1.getDistance(), (int) t2.getDistance()));
            viewModel.getEvStationList().postValue(evStationList);
        }
        /*if(evList.size() > 0)*/
        index++;

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

    @Override
    public void notifyEvStationError(Exception e) {
        viewModel.getExceptionMessage().postValue(String.valueOf(e));
    }
}
