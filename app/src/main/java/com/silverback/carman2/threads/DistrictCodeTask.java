package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.OpinetViewModel;

public class DistrictCodeTask extends ThreadTask
        implements DistrictCodeRunnable.OpinetDistCodeMethods {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(DistrictCodeTask.class);

    // Objects
    private OpinetViewModel model;
    private Runnable opinetDistCodeRunnable;

    // Constructor
    DistrictCodeTask(Context context, OpinetViewModel model) {
        super(); // ThreadTask
        this.model = model;
        opinetDistCodeRunnable = new DistrictCodeRunnable(context, this);
    }

    // Getter for the Runnable invoked by startGasPriceTask() in ThreadManager
    Runnable getOpinetDistCodeRunnable() {
        return opinetDistCodeRunnable;
    }

    void recycle() {}

    @Override
    public void setDistCodeDownloadThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public void hasDistCodeSaved(boolean b) {
        if(b) model.distCodeComplete().postValue(true);
        else model.distCodeComplete().postValue(false);
    }

    /*
    @Override
    public void handleDistCodeTask(int state) {
        int outstate = -1;

        switch(state) {
            case DistrictCodeRunnable.DOWNLOAD_DISTCODE_SUCCEED:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_COMPLTETED;
                break;

            case DistrictCodeRunnable.DOWNLOAD_DISTCODE_FAIL:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

     */

}
