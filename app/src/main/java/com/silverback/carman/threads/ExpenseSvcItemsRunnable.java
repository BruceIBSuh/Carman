package com.silverback.carman.threads;

import android.os.Process;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

public class ExpenseSvcItemsRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseSvcItemsRunnable.class);
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
    ExpenseSvcItemsRunnable(ServiceItemsMethods task) {
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
            task.handleRecyclerTask(TASK_COMPLETE);

        } catch(JSONException e) {
            //log.i("JSONException: %s", e.getMessage());
            task.handleRecyclerTask(TASK_FAIL);
        }

    }
}
