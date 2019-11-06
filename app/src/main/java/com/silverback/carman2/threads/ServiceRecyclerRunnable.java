package com.silverback.carman2.threads;

import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

public class ServiceRecyclerRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceRecyclerRunnable.class);
    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL = -1;

    // Objects
    private RecyclerAdapterMethods task;


    // Interface
    public interface RecyclerAdapterMethods {
        void setRecyclerAdapterThread(Thread thread);
        void setJsonServiceArray(JSONArray jsonArray);
        void handleRecyclerTask(int state);
        String getJsonServiceItems();

    }

    // Constructor
    ServiceRecyclerRunnable(RecyclerAdapterMethods task) {
        this.task = task;
    }

    @Override
    public void run() {

        task.setRecyclerAdapterThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String jsonItemsString = task.getJsonServiceItems();

        try {
            JSONArray jsonArray = new JSONArray(jsonItemsString);
            task.setJsonServiceArray(jsonArray);

        } catch(JSONException e) {
            log.i("JSONException: %s", e.getMessage());
            task.handleRecyclerTask(TASK_FAIL);
        }

    }
}
