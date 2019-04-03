package com.silverback.carman2.threads;

import android.app.Activity;
import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class SaveDistCodeTask extends ThreadTask
        implements SaveDistCodeRunnable.OpinetDistCodeMethods {

    // Logging
    private final LoggingHelper log = LoggingHelperFactory.create(SaveDistCodeTask.class);

    // Objects
    private WeakReference<Activity> mWeakActivity;
    private Runnable opinetDistCodeRunnable;

    // Constructor
    SaveDistCodeTask(Context context) {
        super(); // ThreadTask
        mWeakActivity = new WeakReference<>((Activity)context);
        opinetDistCodeRunnable = new SaveDistCodeRunnable(context, this);
    }

    // Getter for the Runnable invoked by startPriceTask() in ThreadManager
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
            case SaveDistCodeRunnable.DOWNLOAD_DISTCODE_SUCCEED:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_COMPLTETED;
                break;

            case SaveDistCodeRunnable.DOWNLOAD_DISTCODE_FAIL:
                outstate = ThreadManager.DOWNLOAD_DISTCODE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}