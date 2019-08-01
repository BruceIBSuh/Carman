package com.silverback.carman2.threads;

import android.os.Bundle;
import android.os.Process;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman2.adapters.ExpenseTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;

public class ViewPagerRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(ViewPagerRunnable.class);

    // Constants
    static final int FRAGMENT_GAS_COMPLETE = 1;
    static final int FRAGMENT_GAS_FAIL = -1;

    // Objects
    private ViewPagerMethods task;


    public interface ViewPagerMethods {
        void setViewPagerTaskThread(Thread thread);
        void setViewPagerAdapter(ExpenseTabPagerAdapter adapter);
        String[] getDefaults();
        FragmentManager getFragmentManager();
    }


    // Constructor
    ViewPagerRunnable(ViewPagerMethods task) {
        this.task = task;
    }

    @Override
    public void run() {
        task.setViewPagerTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        ExpenseTabPagerAdapter pagerAdapter = new ExpenseTabPagerAdapter(task.getFragmentManager());

        task.getDefaults()[1] = Constants.MIN_RADIUS;
        Bundle args = new Bundle();
        args.putStringArray("defaultParams", task.getDefaults());
        pagerAdapter.getItem(0).setArguments(args);

        task.setViewPagerAdapter(pagerAdapter);

        //pagerAdapter.getPagerFragments()[1] = new ServiceManagerFragment();
        //pagerAdapter.getPagerFragments()[2] = new StatStmtsFragment();

    }
}
