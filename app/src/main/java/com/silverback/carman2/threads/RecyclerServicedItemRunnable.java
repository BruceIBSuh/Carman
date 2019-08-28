package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import androidx.lifecycle.LiveData;

import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class RecyclerServicedItemRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(RecyclerServicedItemRunnable.class);

    // Objects
    private Context context;
    private CarmanDatabase mDB;
    private RecyclerServicedItemMethods task;
    private List<String> svcItemList;
    private List<ServiceManagerDao.ServicedItemData> svcItemData;

    // Interface
    public interface RecyclerServicedItemMethods {
        void setServicedItemThread(Thread thread);
        void setServiceItemList(List<String> itemList);
        String getServiceItem();
        String getServiceItems();
    }

    // Constructor
    RecyclerServicedItemRunnable(Context context, RecyclerServicedItemMethods task) {
        this.context = context;
        this.task = task;
        svcItemList = new ArrayList<>(0);
        mDB = CarmanDatabase.getDatabaseInstance(context.getApplicationContext());
        svcItemData = new ArrayList<>();
    }

    @Override
    public void run() {
        task.setServicedItemThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);


        String jsonItem = task.getServiceItems();

        try {
            JSONArray jsonSvcItemArray = new JSONArray(jsonItem);
            for(int i = 0; i < jsonSvcItemArray.length(); i++) {
                String itemName = jsonSvcItemArray.optJSONObject(i).optString("name");
                log.i("Service Item: %s, %s", itemName, jsonSvcItemArray.optJSONObject(i).optString("mileage"));
                svcItemList.add(itemName);
            }

            task.setServiceItemList(svcItemList);

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }
    }
}
