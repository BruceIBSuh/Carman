package com.silverback.carman.threads;

import androidx.lifecycle.ViewModel;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ThreadTask {

    private static final LoggingHelper log = LoggingHelperFactory.create(ThreadTask.class);

    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL = -1;

    // Objects
    protected ThreadManager2 sThreadManager;
    private Thread mCurrentThread;
    Thread mThreadThis;

    // Constructor
    public ThreadTask(){
        sThreadManager = ThreadManager2.getInstance();
    }

    void recycle() {
        log.i("recycle task");
    }

    /*
     * Returns the Thread that this Task is running on. The method must first get a lock on a
     * static field, in this case the ThreadPool singleton. The lock is needed because the
     * Thread object reference is stored in the Thread object itself, and that object can be
     * changed by processes outside of this app.
     */
    protected Thread getCurrentThread() {
        log.d("ThreadTask current Thread: %s", mCurrentThread);
        return mCurrentThread;
    }
    /*
     * Sets the identifier for the current Thread. This must be a synchronized operation; see the
     * notes for getCurrentThread()
     */
    protected void setCurrentThread(Thread thread) {
        mCurrentThread = thread;
    }

    public void handleTaskState(ThreadTask task, int state) {
        sThreadManager.handleState(task, state);
    }



}