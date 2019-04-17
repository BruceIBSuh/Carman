package com.silverback.carman2.threads;

import android.os.Process;

public class FireStoreGetRunnable implements Runnable {

    // Objects
    private FireStoreGetMethods task;


    public interface FireStoreGetMethods {
        void setStationTaskThread(Thread thread);
    }

    FireStoreGetRunnable(FireStoreGetMethods task) {
        this.task = task;
    }

    @Override
    public void run() {

        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

    }
}
