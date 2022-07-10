package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.OpinetViewModel;

import java.util.List;

public class DistSpinnerTask extends ThreadTask implements
        DistSpinnerRunnable.DistCodeMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DistSpinnerTask.class);

    // Objects
    private OpinetViewModel model;
    private final DistSpinnerRunnable distSpinnerRunnable;
    private int sidoCode;

    // Constructor
    DistSpinnerTask(Context context) {
        distSpinnerRunnable = new DistSpinnerRunnable(context, this);
    }

    void initSpinnerDistCodeTask(OpinetViewModel model, int sidoCode) {
        this.sidoCode = sidoCode;
        this.model = model;
        log.i("sido code in task: %s", sidoCode);
    }

    Runnable getDistCodeSpinnerRunnable() {
        return distSpinnerRunnable;
    }

    public void recycle() {
        sidoCode = -1;
    }

    @Override
    public void setSpinnerDistCodeThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public int getSidoCode() {
        return sidoCode;
    }

    // Post(Set) value in SpinnerDistriceModel, which is notified to the parent fragment,
    // SettingSpinnerDlgFragment as LiveData.
    @Override
    public void setSigunCode(List<DistDownloadRunnable.Area> distCode) {
        log.i("Dist Code: %s", distCode.size());
        model.getSpinnerDataList().postValue(distCode);
    }

    @Override
    public void handleDistCodeSpinnerTask(int state) {
        int outState = -1;
        switch(state) {
            case DistSpinnerRunnable.SPINNER_DIST_CODE_COMPLETE:
                outState = ThreadManager.LOAD_SPINNER_DIST_CODE_COMPLETE;
                break;
            case DistSpinnerRunnable.SPINNER_DIST_CODE_FAIL:
                outState = ThreadManager.LOAD_SPINNER_DIST_CODE_FAILED;
                break;
        }
        sThreadManager.handleState(this, outState);
    }



}
