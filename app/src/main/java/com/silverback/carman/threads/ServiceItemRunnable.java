package com.silverback.carman.threads;

import android.content.Context;
import android.os.Process;

import androidx.lifecycle.LiveData;

import com.silverback.carman.adapters.ExpServiceItemAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.database.ServiceManagerDao;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

public class ServiceItemRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceItemRunnable.class);

    private final ServiceItemMethods mTask;
    private final CarmanDatabase mDB;
    private ExpServiceItemAdapter mAdapter;
    private Context context;

    public interface ServiceItemMethods {
        void setServiceItemsThread(Thread thread);
        String getJsonServiceItems();
    }

    // Constructor
    public ServiceItemRunnable(Context context, ServiceItemMethods task) {
        mTask = task;
        this.context = context;
        mDB = CarmanDatabase.getDatabaseInstance(context);

    }

    @Override
    public void run() {
        mTask.setServiceItemsThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String jsonItemString = mTask.getJsonServiceItems();

        try {
            JSONArray jsonArray = new JSONArray(jsonItemString);
            for(int i = 0; i < jsonArray.length(); i++) {
                final int pos = i;
                final String name = jsonArray.optJSONObject(pos).getString("name");
                LiveData<ServiceManagerDao.LatestServiceData> data =
                        mDB.serviceManagerModel().loadServiceData(name);
            }

        } catch(JSONException e) {
            log.e("JSONException: %s", e);
        }

    }
}
