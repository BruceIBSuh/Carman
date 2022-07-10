package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.OpinetViewModel;

public class DistDownloadTask extends ThreadTask
        implements DistDownloadRunnable.OpinetDistCodeMethods {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(DistDownloadTask.class);

    // Objects
    private final OpinetViewModel model;
    private final Runnable opinetDistCodeRunnable;

    // Constructor
    public DistDownloadTask(Context context, OpinetViewModel model) {
        super(); // ThreadTask
        this.model = model;
        opinetDistCodeRunnable = new DistDownloadRunnable(context, this);
    }

    // Getter for the Runnable invoked by startGasPriceTask() in ThreadManager
    Runnable getOpinetDistCodeRunnable() {
        return opinetDistCodeRunnable;
    }

    protected void recycle() {
        log.i("override recycler method in child task");
    }

    @Override
    public void setDistCodeDownloadThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public void hasDistCodeSaved(boolean b) {
        log.i("DistDownloadTask done: %s", b);
        model.distCodeComplete().postValue(b);
    }

    @Override
    public void handleDistCodeTask(int state) {
        int outstate = -1;
        switch(state) {
            case DistDownloadRunnable.TASK_COMPLETE:
                outstate = ThreadManager2.DISTCODE_COMPLETED;
                break;

            case DistDownloadRunnable.TASK_FAIL:
                outstate = ThreadManager2.DISTCODE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }
}
