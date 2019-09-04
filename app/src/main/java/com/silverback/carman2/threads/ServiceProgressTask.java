package com.silverback.carman2.threads;

import android.content.Context;
import android.util.SparseArray;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ServiceProgressTask extends ThreadTask implements ServiceProgressRunnable.ProgressBarAnimMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceProgressTask.class);

    //
    private Runnable mServiceProgressRunnable;
    private String name;
    private int key;


    public ServiceProgressTask(Context context) {
        super();
        mServiceProgressRunnable = new ServiceProgressRunnable(context, this);

    }

    public void initTask(String name, int key) {
        this.name = name;
        this.key = key;
    }

    Runnable getServiceProgressRunnable() {
        return mServiceProgressRunnable;
    }

    void recycle() {}

    @Override
    public void setProgressBarAnimThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public SparseArray<String> getSparseServiceItemArray() {
        SparseArray<String> sparseArray = new SparseArray<>();
        sparseArray.put(key, name);
        return sparseArray;
    }
}
