package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.adapters.ServiceItemAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

public class RecyclerAdapterRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(RecyclerAdapterRunnable.class);
    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL = -1;

    // Objects
    private RecyclerAdapterMethods task;


    // Interface
    public interface RecyclerAdapterMethods {
        void setRecyclerAdapterThread(Thread thread);
        void setRecyclerAdapter(ServiceItemAdapter adapter);
        void handleRecyclerTask(int state);
        String getServiceItems();

    }

    // Constructor
    RecyclerAdapterRunnable(RecyclerAdapterMethods task) {
        this.task = task;
    }



    @Override
    public void run() {
        log.i("RecyclerAdapterRunnable");
        task.setRecyclerAdapterThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String jsonItems = task.getServiceItems();
        try {
            JSONArray jsonArray = new JSONArray(jsonItems);
            ServiceItemAdapter adapter = new ServiceItemAdapter(jsonArray);
            task.setRecyclerAdapter(adapter);
            task.handleRecyclerTask(TASK_COMPLETE);
        } catch(JSONException e) {
            log.i("JSONException: %s", e.getMessage());
            task.handleRecyclerTask(TASK_FAIL);
        }

    }
}
