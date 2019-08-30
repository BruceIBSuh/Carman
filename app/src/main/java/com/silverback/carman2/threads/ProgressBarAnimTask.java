package com.silverback.carman2.threads;

import android.widget.ProgressBar;

import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.PagerAdapterViewModel;

import java.lang.ref.WeakReference;

public class ProgressBarAnimTask extends ThreadTask implements ProgressBarAnimRunnable.ProgressBarAnimMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ProgressBarAnimTask.class);

    // Objects
    private PagerAdapterViewModel model;
    private ServiceManagerDao.LatestServiceData data;
    private int position, period;
    private Runnable mProgressBarAnimRunnable;

    // UIs
    private WeakReference<ProgressBar> weakProgressBarRef;

    ProgressBarAnimTask() {
        super();
        mProgressBarAnimRunnable = new ProgressBarAnimRunnable(this);
    }

    void initAnimTask(ProgressBar progbar, int position, int period, PagerAdapterViewModel model) {

        weakProgressBarRef = new WeakReference<>(progbar);
        this.model = model;
        this.position = position;
        this.period = period;
    }

    Runnable getProgressBarAnimRunnable() {
        return mProgressBarAnimRunnable;
    }

    @Override
    public int getServicePosition() {
        return position;
    }

    @Override
    public int getServicePeriod() {
        return period;
    }

    @Override
    public WeakReference<ProgressBar> getProgressBar() {
        return null;
    }

    @Override
    public void setProgressValue(int pos, float value) {
        model.getProgressValue().postValue((int)value);
    }

    @Override
    public void setProgressBarAnimThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void handeProgressBarAnimTask(int state) {

    }
}
