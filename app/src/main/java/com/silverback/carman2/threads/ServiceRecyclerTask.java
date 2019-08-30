package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.adapters.ExpServiceItemAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.PagerAdapterViewModel;

import org.json.JSONArray;

import java.util.List;

public class ServiceRecyclerTask extends ThreadTask implements
        ServiceRecyclerRunnable.RecyclerAdapterMethods,
        ServiceStmtsRecyclerRunnable.RecyclerServicedItemMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceRecyclerTask.class);

    // Objects
    private Context context;
    private PagerAdapterViewModel model;
    private String jsonServiceItems;
    private Runnable recyclerAdapterRunnable;
    private Runnable recyclerServicedItemRunnable;

    // Fields
    private String svcItemName;


    // Constructor
    ServiceRecyclerTask() {
        super();
        recyclerAdapterRunnable = new ServiceRecyclerRunnable(this);
        recyclerServicedItemRunnable = new ServiceStmtsRecyclerRunnable(context, this);
    }

    void initTask(PagerAdapterViewModel model, String json) {
        this.model = model;
        jsonServiceItems = json;
    }

    Runnable getRecyclerAdapterRunnable() {
        return recyclerAdapterRunnable;
    }
    Runnable getRecyclerServicedItemRunnable() {
        return recyclerServicedItemRunnable;
    }

    void recycle() {}

    // Set the current thread of ServiceRecyclerRunnable
    @Override
    public void setRecyclerAdapterThread(Thread thread) {
        setCurrentThread(thread); // defined in the super classs, ThreadTask.
    }

    // Set the current thread of ServiceStmtsRecyclerRunnable
    @Override
    public void setServicedItemThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setRecyclerAdapter(ExpServiceItemAdapter adapter) {
        model.getServiceAdapter().postValue(adapter);
    }

    @Override
    public void setJsonServiceArray(JSONArray jsonArray) {
        model.getJsonServiceArray().postValue(jsonArray);
    }

    @Override
    public void setServiceItemList(List<String> itemList) {
        model.getServicedItem().postValue(itemList);
    }

    @Override
    public String getServiceItems() {
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
