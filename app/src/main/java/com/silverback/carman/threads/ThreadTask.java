package com.silverback.carman.threads;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class ThreadTask {

    private static final LoggingHelper log = LoggingHelperFactory.create(ThreadTask.class);

    // Objects
    static ThreadManager sThreadManager;
    private Thread mCurrentThread;
    Thread mThreadThis;

    //Constructor
    ThreadTask(){
        sThreadManager = ThreadManager.getInstance();
    }

    /*
     * Returns the Thread that this Task is running on. The method must first get a lock on a
     * static field, in this case the ThreadPool singleton. The lock is needed because the
     * Thread object reference is stored in the Thread object itself, and that object can be
     * changed by processes outside of this app.
     */
    public Thread getCurrentThread() {
        log.d("ThreadTask current Thread: %s", mCurrentThread);
        return mCurrentThread;
    }
    /*
     * Sets the identifier for the current Thread. This must be a synchronized operation; see the
     * notes for getCurrentThread()
     */
    void setCurrentThread(Thread thread) {
        //synchronized(sThreadManager) {
            mCurrentThread = thread;
            log.d("ThreadTask current Thread: %s", mCurrentThread);
        //}
    }

}