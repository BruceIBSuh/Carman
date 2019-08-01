package com.silverback.carman2.threads;

import android.app.Activity;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProviders;

import com.silverback.carman2.adapters.ExpenseTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ViewPagerModel;

import java.lang.ref.WeakReference;

public class ViewPagerTask extends ThreadTask implements ViewPagerRunnable.ViewPagerMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(ViewPagerTask.class);

    // Objects
    private FragmentManager fragmentManager;
    private ViewPagerModel pagerModel;
    private String[] defaults;

    // Constructor
    ViewPagerTask() {
        super();
    }

    void initViewPagerTask(ViewPagerModel viewModel, FragmentManager fm, String[] defaults) {
        fragmentManager = fm;
        pagerModel = viewModel;
        this.defaults = defaults;
    }

    Runnable getViewPagerRunnable() {
        return new ViewPagerRunnable(this);
    }


    @Override
    public void setViewPagerTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    @Override
    public void setViewPagerAdapter(ExpenseTabPagerAdapter adapter) {
        log.i("FragmentStatePagerAdapter in background thread: %s", adapter);
        pagerModel.getPagerAdapter().postValue(adapter);
    }

    @Override
    public String[] getDefaults() {
        return defaults;
    }

    void recycle() {

    }


}
