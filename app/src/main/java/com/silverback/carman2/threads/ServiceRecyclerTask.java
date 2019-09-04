package com.silverback.carman2.threads;

import android.content.Context;
import android.util.SparseArray;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.PagerAdapterViewModel;

import org.json.JSONArray;

public class ServiceRecyclerTask extends ThreadTask implements
        ServiceRecyclerRunnable.RecyclerAdapterMethods,
        ServiceProgressRunnable.ProgressBarAnimMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceRecyclerTask.class);

    // Objects
    private Context context;
    private PagerAdapterViewModel model;
    private String jsonServiceItems;
    private Runnable recyclerAdapterRunnable;
    //private Runnable recyclerServicedItemRunnable;
    private Runnable progbarAnimRunnable;
    private SparseArray<String> sparseSvcItemArray;

    // Fields

    private int svcItemPos;
    private String svcItemName;


    // Constructor
    ServiceRecyclerTask(Context context) {
        super();
        this.context = context;
        recyclerAdapterRunnable = new ServiceRecyclerRunnable(this);
        //recyclerServicedItemRunnable = new ServiceStmtsRecyclerRunnable(context, this);
        progbarAnimRunnable = new ServiceProgressRunnable(context, this);
    }

    void initTask(PagerAdapterViewModel model, String json) {
        this.model = model;
        jsonServiceItems = json;
    }

    Runnable getRecyclerAdapterRunnable() {
        return recyclerAdapterRunnable;
    }

    /*
    Runnable getRecyclerServicedItemRunnable() {
        return recyclerServicedItemRunnable;
    }
    */

    Runnable getProgbarAnimRunnable() {
        return progbarAnimRunnable;
    }

    void recycle() {}

    // Set the current thread of ServiceRecyclerRunnable
    @Override
    public void setRecyclerAdapterThread(Thread thread) {
        setCurrentThread(thread); // defined in the super classs, ThreadTask.
    }

    @Override
    public void setProgressBarAnimThread(Thread thread) {

    }

    @Override
    public void setJsonServiceArray(JSONArray jsonArray) {
        model.getJsonServiceArray().postValue(jsonArray);
    }

    @Override
    public synchronized void setServiceItem(int pos, String name) {
        log.i("set service item: %s, %s", pos, name);
        SparseArray<String> sparseArray = new SparseArray<>();
        sparseArray.put(pos, name);
        sparseSvcItemArray = sparseArray;
    }

    @Override
    public String getJsonServiceItems() {
        return jsonServiceItems;
    }

    @Override
    public synchronized SparseArray<String> getSparseServiceItemArray() {
        return sparseSvcItemArray;
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
