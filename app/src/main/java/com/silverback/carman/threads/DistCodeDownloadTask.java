package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.OpinetViewModel;

public class DistCodeDownloadTask extends ThreadTask
        implements DistCodeDownloadRunnable.OpinetDistCodeMethods {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(DistCodeDownloadTask.class);

    // Objects
    private final OpinetViewModel model;
    private final Runnable opinetDistCodeRunnable;

    // Constructor
    public DistCodeDownloadTask(Context context, OpinetViewModel model) {
        super(); // ThreadTask
        this.model = model;
        opinetDistCodeRunnable = new DistCodeDownloadRunnable(context, this);
    }

    // Getter for the Runnable invoked by startGasPriceTask() in ThreadManager
    Runnable getOpinetDistCodeRunnable() {
        return opinetDistCodeRunnable;
    }

    @Override
    protected void recycle() {
        log.i("override recycler method in child task");
    }

    @Override
    public void setDistCodeDownloadThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public void hasDistCodeSaved(boolean b) {
        model.distCodeComplete().postValue(b);
    }

    @Override
    public void handleDistCodeTask(int state) {
        handleTaskState(this, state);
        /*
        int outstate = -1;
        switch(state) {
            case DistCodeDownloadRunnable.TASK_COMPLETE:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_COMPLETED;
                break;

            case DistCodeDownloadRunnable.TASK_FAIL:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);

         */
    }
}
