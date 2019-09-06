package com.silverback.carman2.threads;

import android.content.Context;
import android.util.SparseArray;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.PagerAdapterViewModel;

import org.json.JSONArray;

public class ServiceRecyclerTask extends ThreadTask implements
        ServiceRecyclerRunnable.RecyclerAdapterMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceRecyclerTask.class);

    // Objects
    private PagerAdapterViewModel model;
    private String jsonServiceItems;
    private Runnable serviceRecyclerRunnable;


    // Constructor
    ServiceRecyclerTask() {
        super();
        serviceRecyclerRunnable = new ServiceRecyclerRunnable(this);
    }

    void initTask(PagerAdapterViewModel model, String json) {
        this.model = model;
        jsonServiceItems = json;
    }

    Runnable getServiceRecyclerRunnable() {
        return serviceRecyclerRunnable;
    }

    void recycle() {}

    // Set the current thread of ServiceRecyclerRunnable
    @Override
    public void setRecyclerAdapterThread(Thread thread) {
        setCurrentThread(thread); // defined in the super classs, ThreadTask.
    }

    @Override
    public void setJsonServiceArray(JSONArray jsonArray) {
        model.getJsonServiceArray().postValue(jsonArray);
    }

    @Override
    public String getJsonServiceItems() {
        return jsonServiceItems;
    }

    @Override
    public void handleRecyclerTask(int state) {
        log.i("handleRecyclerTask");
        int outstate = -1;
        switch(state) {
            case ServiceRecyclerRunnable.TASK_COMPLETE:
                outstate = ThreadManager.RECYCLER_ADAPTER_SERVICE_COMPLETED;
                break;

            case ServiceRecyclerRunnable.TASK_FAIL:
                outstate = ThreadManager.RECYCLER_ADAPTER_SERVICE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }


}
