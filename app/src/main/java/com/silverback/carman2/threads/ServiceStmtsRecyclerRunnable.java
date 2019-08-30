package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ServiceStmtsRecyclerRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceStmtsRecyclerRunnable.class);

    // Objects
    private RecyclerServicedItemMethods task;
    private List<String> svcItemList;

    // Interface
    public interface RecyclerServicedItemMethods {
        void setServicedItemThread(Thread thread);
        void setServiceItemList(List<String> itemList);
        String getServiceItems();
    }

    // Constructor
    ServiceStmtsRecyclerRunnable(Context context, RecyclerServicedItemMethods task) {
        this.task = task;
        svcItemList = new ArrayList<>(0);
    }

    @Override
    public void run() {
        task.setServicedItemThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        final String jsonItem = task.getServiceItems();

        try {
            JSONArray jsonSvcItemArray = new JSONArray(jsonItem);

            synchronized (this) {
                for (int i = 0; i < jsonSvcItemArray.length(); i++) {
                    String itemName = jsonSvcItemArray.optJSONObject(i).optString("name");
                    svcItemList.add(itemName);
                }
            }

            task.setServiceItemList(svcItemList);

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }
    }
}
