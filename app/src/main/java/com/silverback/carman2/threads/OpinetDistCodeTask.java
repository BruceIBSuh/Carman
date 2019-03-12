package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class OpinetDistCodeTask extends ThreadTask
        implements OpinetDistCodeRunnable.OpinetDistCodeMethods {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(OpinetDistCodeTask.class);

    // Objects
    private WeakReference<Activity> mWeakActivity;
    private Runnable opinetDistCodeRunnable;

    // Constructor
    OpinetDistCodeTask(Context context) {
        super(); // ThreadTask
        mWeakActivity = new WeakReference<>((Activity)context);
        opinetDistCodeRunnable = new OpinetDistCodeRunnable(context, this);
    }

    // Getter for the Runnable invoked by startOpinetPriceTask() in ThreadManager
    Runnable getOpinetDistCodeRunnable() {
        return opinetDistCodeRunnable;
    }

    void recycle() {
        if(mWeakActivity != null) {
            mWeakActivity.clear();
            mWeakActivity = null;
        }
    }

    @Override
    public void setDistCodeDownloadThread(Thread currentThread) {
        log.i("Current Thread: " + currentThread);
        // Inheritedd from the parent class of ThreadTask
        setCurrentThread(currentThread);
    }

    @Override
    public void handleDistCodeTask(int state) {
        int outstate = -1;

        switch(state) {
            case OpinetDistCodeRunnable.DOWNLOAD_DISTCODE_SUCCEED:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_COMPLTETED;
                break;

            case OpinetDistCodeRunnable.DOWNLOAD_DISTCODE_FAIL:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}
