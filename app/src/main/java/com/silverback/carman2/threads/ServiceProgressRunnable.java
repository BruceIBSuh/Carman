package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;
import android.util.SparseArray;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class ServiceProgressRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceProgressRunnable.class);

    // Objects
    private CarmanDatabase mDB;
    private ProgressBarAnimMethods mCallback;
    private SparseArray<String> sparseSvcItemArray;
    private WeakReference<ProgressBar> weakProgressBarRef;

    // Interface
    public interface ProgressBarAnimMethods {
        void setProgressBarAnimThread(Thread thread);
        SparseArray<String> getSparseServiceItemArray();
        /*
        int getServicePosition();
        int getServicePeriod();
        WeakReference<ProgressBar> getProgressBar();

        void setProgressValue(int position, float value);

        void handeProgressBarAnimTask(int state);
        */
    }

    // Constructor
    ServiceProgressRunnable(Context context, ProgressBarAnimMethods callback) {

        mCallback = callback;
        mDB = CarmanDatabase.getDatabaseInstance(context.getApplicationContext());
    }

    @Override
    public void run() {
        mCallback.setProgressBarAnimThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        sparseSvcItemArray = mCallback.getSparseServiceItemArray();



        /*
        int pos = mCallback.getServicePosition();
        int period = mCallback.getServicePeriod();

        ProgressBarAnimation anim = new ProgressBarAnimation(0, period);
        anim.setDuration(1000);
        float value = anim.getProgressValue();
        log.i("Progress Value: %s", value);
        */
    }

    // ProgressBar Animation class from StackOverflow
    class ProgressBarAnimation extends Animation {

        private ProgressBar progressBar;
        private float from;
        private float to;
        private float value;

        ProgressBarAnimation(float from, float to) {
            super();
            //this.progressBar = progressBar;
            this.from = from;
            this.to = to;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            value = from + (to - from) * interpolatedTime;
            log.i("VALUE: %s", value);
            //progressBar.setProgress((int)value);
        }

        float getProgressValue() {
            return value;
        }
    }
}
