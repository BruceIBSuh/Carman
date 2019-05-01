package com.silverback.carman2.threads;

import android.content.Context;
import android.view.View;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class ClockTask extends ThreadTask implements ClockRunnable.ClockMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ClockTask.class);

    // Objects
    private static ThreadManager sThreadManager;
    private WeakReference<View> mWeakView;
    private Runnable mClockRunnable;

    // Fields
    private String currentTime;

    // Constructor
    ClockTask(Context context) {
        mClockRunnable = new ClockRunnable(context, this);
    }


    void initClockTask(ThreadManager threadManager, View view) {
        sThreadManager = threadManager;
        mWeakView = new WeakReference<>(view);
    }

    @Override
    public void setClockTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setCurrentTime(String time) {
        currentTime = time;
    }

    @Override
    public void handleClockTaskState() {
        sThreadManager.handleState(this, ThreadManager.UPDATE_CLOCK);
    }

    Runnable getClockRunnable() {
        return mClockRunnable;
    }

    View getClockView() {
        return mWeakView.get();
    }

    String getCurrentTime() {
        return currentTime;
    }

    void recycle() {
        if(mWeakView != null) {
            mWeakView.clear();
            mWeakView = null;
        }
    }


}
