package com.silverback.carman.threads;

import android.content.Context;

import com.silverback.carman.database.CarmanDatabase;

public class ServiceItemTask extends ThreadTask implements ServiceItemRunnable.ServiceItemMethods {

    private String jsonServiceItems;
    private final ServiceItemRunnable mServiceItemRunnable;

    public ServiceItemTask(Context context) {
        mServiceItemRunnable = new ServiceItemRunnable(context, this);
    }

    public void init(String jsonServiceItems) {
        this.jsonServiceItems = jsonServiceItems;
    }

    ServiceItemRunnable getServiceItemRunnable() {
        return mServiceItemRunnable;
    }

    @Override
    public void setServiceItemsThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public String getJsonServiceItems() {
        return jsonServiceItems;
    }
}
