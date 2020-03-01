package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;

public class FirestoreResTask extends ThreadTask implements FirestoreResRunnable.FirestoreResMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FirestoreResTask.class);

    // Objects
    private FirestoreViewModel model;
    private Runnable mFirestoreResRunnable;

    // Constructor
    FirestoreResTask(Context context){
        super();
        mFirestoreResRunnable = new FirestoreResRunnable(context, this);

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
            case FirestoreResRunnable.DOWNLOAD_COMPLETED:
                model.getResTaskDone().postValue(true);
                break;

            case FirestoreResRunnable.DOWNLAOD_FAILED:
                model.getResTaskDone().postValue(false);
                break;

            default:break;
        }

        // Temporarily outState is not applied with the transferred state.
        sThreadManager.handleState(this, outState);
    }
}


