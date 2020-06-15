package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.OpinetViewModel;

public class DistCodeDownloadTask extends ThreadTask
        implements DistCodeDownloadRunnable.OpinetDistCodeMethods {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(DistCodeDownloadTask.class);



    // Objects
    private OpinetViewModel model;
    private Runnable opinetDistCodeRunnable;

    // Constructor
    DistCodeDownloadTask(Context context, OpinetViewModel model) {
        super(); // ThreadTask
        this.model = model;
        opinetDistCodeRunnable = new DistCodeDownloadRunnable(context, this);
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
        int outstate = -1;
        if(b) model.distCodeComplete().postValue(true);
        else model.distCodeComplete().postValue(false);

        sThreadManager.handleState(this, outstate);
    }


    @Override
    public void handleDistCodeDownload(int state) {
        int outstate = -1;
        switch(state) {
            case DistCodeDownloadRunnable.DISTRICT_CODE_COMPLETE:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_COMPLETED;
                break;

            case DistCodeDownloadRunnable.DISTRICT_CODE_FAIL:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }



}
