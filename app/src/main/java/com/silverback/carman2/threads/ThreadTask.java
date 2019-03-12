package com.silverback.carman2.threads;

public class ThreadTask {

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
    public synchronized Thread getCurrentThread() {
        //synchronized(sThreadManager) {
        //Log.d(LOG_TAG, "ThreadTask current Thread: " + mCurrentThread);
        return mCurrentThread;
        //}
    }
    /*
     * Sets the identifier for the current Thread. This must be a synchronized operation; see the
     * notes for getCurrentThread()
     */
    synchronized void setCurrentThread(Thread thread) {
        //synchronized(sThreadManager) {
        //Log.d(LOG_TAG, "ThreadTask current Thread: " + mCurrentThread);
        mCurrentThread = thread;
        //}
    }

}