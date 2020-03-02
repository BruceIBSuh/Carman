package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;

public class AutoDataResourceTask extends ThreadTask implements AutoDataResourceRunnable.FirestoreResMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(AutoDataResourceTask.class);

    // Objects
    private FirestoreViewModel model;
    private Runnable mFirestoreResRunnable;

    // Constructor
    AutoDataResourceTask(Context context){
        super();
        mFirestoreResRunnable = new AutoDataResourceRunnable(context, this);

    }

    void initResourceTask(FirestoreViewModel model) {
        this.model = model;
    }

    Runnable getFirestoreResRunnable() {
        return mFirestoreResRunnable;
    }

    void recycle() {}

    @Override
    public void setResourceThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void handleState(int state) {
        int outState = 0;
        switch(state) {
            case AutoDataResourceRunnable.DOWNLOAD_COMPLETED:
                model.getResTaskDone().postValue(true);
                break;

            case AutoDataResourceRunnable.DOWNLOAD_FAILED:
                model.getResTaskDone().postValue(false);
                break;

            default:break;
        }

        // Temporarily outState is not applied with the transferred state.
        sThreadManager.handleState(this, outState);
    }
}


