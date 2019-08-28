package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.adapters.ExpServiceItemAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.PagerAdapterViewModel;

import java.util.List;

public class RecyclerAdapterTask extends ThreadTask implements
        RecyclerAdapterRunnable.RecyclerAdapterMethods,
        RecyclerServicedItemRunnable.RecyclerServicedItemMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(RecyclerAdapterTask.class);

    // Objects
    private Context context;
    private PagerAdapterViewModel model;
    private String jsonServiceItems;
    private Runnable recyclerAdapterRunnable;
    private Runnable recyclerServicedItemRunnable;

    // Fields
    private String svcItemName;


    // Constructor
    RecyclerAdapterTask(Context context) {
        super();

        this.context = context;
        recyclerAdapterRunnable = new RecyclerAdapterRunnable(this);
        recyclerServicedItemRunnable = new RecyclerServicedItemRunnable(context, this);
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

    // Set the current thread of RecyclerAdapterRunnable
    @Override
    public void setRecyclerAdapterThread(Thread thread) {
        setCurrentThread(thread); // defined in the super classs, ThreadTask.
    }

    // Set the current thread of RecyclerServicedItemRunnable
    @Override
    public void setServicedItemThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setRecyclerAdapter(ExpServiceItemAdapter adapter) {
        model.getServiceAdapter().postValue(adapter);
    }

    @Override
    public void setServiceItemName(String name) {
        svcItemName = name;
    }

    @Override
    public void setServiceItemList(List<String> itemList) {
        model.getServicedItem().postValue(itemList);
    }

    @Override
    public String getServiceItem(){
        return svcItemName;
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
            case RecyclerAdapterRunnable.TASK_COMPLETE:
                outstate = ThreadManager.RECYCLER_ADAPTER_SERVICE_COMPLETED;
                break;

            case RecyclerAdapterRunnable.TASK_FAIL:
                outstate = ThreadManager.RECYCLER_ADAPTER_SERVICE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outstate);
    }

}
