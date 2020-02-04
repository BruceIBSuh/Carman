package com.silverback.carman2.threads;

import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

public class ServiceItemsRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemsRunnable.class);
    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL = -1;

    // Objects
    private ServiceItemsMethods task;


    // Interface
    public interface ServiceItemsMethods {
        void setServiceItemsThread(Thread thread);
        void setJsonSvcArray(JSONArray jsonArray);
        void handleRecyclerTask(int state);
        String getJsonServiceItems();

    }

    // Constructor
    ServiceItemsRunnable(ServiceItemsMethods task) {
        this.task = task;
    }

    @Override
    public void run() {

        task.setServiceItemsThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        String jsonItemsString = task.getJsonServiceItems();

        try {
            JSONArray jsonArray = new JSONArray(jsonItemsString);
            task.setJsonSvcArray(jsonArray);

        } catch(JSONException e) {
            log.i("JSONException: %s", e.getMessage());
            task.handleRecyclerTask(TASK_FAIL);
        }

    }
}
