package com.silverback.carman2.threads;

import android.os.Bundle;
import android.os.Process;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman2.adapters.ExpTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

public class TabPagerRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(TabPagerRunnable.class);


    // Objects
    private ViewPagerMethods task;
    private ExpTabPagerAdapter pagerAdapter;

    public interface ViewPagerMethods {
        void setViewPagerTaskThread(Thread thread);
        void setViewPagerAdapter(ExpTabPagerAdapter adapter);
        String[] getDefaults();
        String getJsonDistrict();
        FragmentManager getFragmentManager();
    }


    // Constructor
    TabPagerRunnable(ViewPagerMethods task) {
        this.task = task;
        pagerAdapter = new ExpTabPagerAdapter(task.getFragmentManager());
    }

    @Override
    public void run() {
        task.setViewPagerTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // Pass the default params to GasManagerFragment in TabPagerAdapter
        task.getDefaults()[1] = Constants.MIN_RADIUS;
        Bundle gasArgs = new Bundle();
        gasArgs.putStringArray("defaultParams", task.getDefaults());
        pagerAdapter.getItem(0).setArguments(gasArgs);

        // Pass the district code to ServiceManagerFragment in TabPagerAdapter
        try {
            JSONArray jsonArray = new JSONArray(task.getJsonDistrict());
            String distCode = (String)jsonArray.get(2);
            Bundle svcArgs = new Bundle();
            svcArgs.putString("distCode", distCode);
            pagerAdapter.getItem(1).setArguments(svcArgs);

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }


        task.setViewPagerAdapter(pagerAdapter);

    }
}
