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

public class StationEvTask extends ThreadTask implements
        StationEvRunnable.ElecStationCallback,
        StationAddrsRunnable.StationAddrsCallback {

    private static final LoggingHelper log = LoggingHelperFactory.create(StationEvTask.class);
    private static final String regexEvName = "\\d*\\([\\w\\s]*\\)";

    static final int EV_TASK_SUCCESS = 1;
    static final int EV_TASK_FAIL = -1;
    static final int EV_ADDRS_TASK_SUCCESS = 2;
    static final int EV_ADDRS_TASK_FAIL = -2;

    private final Runnable mStationAddrsRunnable;
    private final Location location;
    private final Context context;
    private final StationViewModel viewModel;
    private StationAddrsRunnable.EvSidoCode evEnumSidoCode;


    private final List<StationEvRunnable.Item> evStationList;
    private int page = 1;
    private int lastPage;

    public StationEvTask(Context context, StationViewModel viewModel, Location location){
        this.context = context;
        this.location = location;
        this.viewModel = viewModel;

        mStationAddrsRunnable = new StationAddrsRunnable(context, this);
        evStationList = new ArrayList<>();
    }

    public Runnable getStationAddrsRunnable() {
        return mStationAddrsRunnable;
    }
    public Runnable getEvStnListRunnable(int queryPage, int lastPage, int code) {
        this.lastPage = lastPage;
        return new StationEvRunnable(context, queryPage, code, this);
    }

    public void recycle(){
        log.i("recycle StationEvTask");
    }


    @Override
    public void setStationAddrsThread(Thread thread) {
        super.setCurrentThread(thread);
    }

    @Override
    public void setEvStationTaskThread(Thread thread) {
        super.setCurrentThread(thread);
    }

    @Override
    public void setEnumEvSidoCode(StationAddrsRunnable.EvSidoCode evSidoCode) {
        this.evEnumSidoCode = evSidoCode;
    }

    @Override
    public Location getEvStationLocation() {
        return location;
    }


    @Override
    public void setEvStationList(List<StationEvRunnable.Item> evList) {
        if(evList != null && evList.size() > 0) evStationList.addAll(evList);
        if(evList != null) log.i("ev list: %s", evList.size());

        if(page == lastPage){
            // Sort the ev stations in the distance-descending order
            log.i("page: %s, %s", page, evStationList.size());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Collections.sort(evStationList, Comparator.comparingInt(t -> (int) t.getDistance()));
            else Collections.sort(evStationList, (t1, t2) ->
                    Integer.compare((int) t1.getDistance(), (int) t2.getDistance()));

            viewModel.getEvStationList().postValue(evStationList);
            return;
        }
        page++;
    }

    @Override
    public void notifyEvStationError(Exception e) {
        viewModel.getEvExceptionMessage().postValue(String.valueOf(e.getLocalizedMessage()));
        page++;
    }

    @Override
    public void handleTaskState(int state) {
        int outstate = 0;
        switch(state) {
            case EV_ADDRS_TASK_SUCCESS:
                outstate = sThreadManager.EV_ADDRS_SUCCESS;
                break;
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

    StationAddrsRunnable.EvSidoCode getEvEnumSidoCode() {
        return evEnumSidoCode;
    }
}
