package com.silverback.carman2.threads;

import android.os.Bundle;
import android.os.Process;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman2.adapters.ExpTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;

public class TabPagerRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(TabPagerRunnable.class);


    // Objects
    private ViewPagerMethods task;
    private ExpTabPagerAdapter pagerAdapter;

    public interface ViewPagerMethods {
        void setViewPagerTaskThread(Thread thread);
        void setViewPagerAdapter(ExpTabPagerAdapter adapter);
        String[] getDefaults();
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

        //ExpTabPagerAdapter pagerAdapter = new ExpTabPagerAdapter(task.getFragmentManager());

        task.getDefaults()[1] = Constants.MIN_RADIUS;
        Bundle args = new Bundle();
        args.putStringArray("defaultParams", task.getDefaults());
        pagerAdapter.getItem(0).setArguments(args);

        task.setViewPagerAdapter(pagerAdapter);

    }
}
