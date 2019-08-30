package com.silverback.carman2.threads;

import android.os.Process;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.lang.ref.WeakReference;

public class ProgressBarAnimRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ProgressBarAnimRunnable.class);

    // Objects
    private ProgressBarAnimMethods mCallback;
    private WeakReference<ProgressBar> weakProgressBarRef;

    // Interface
    public interface ProgressBarAnimMethods {
        int getServicePosition();
        int getServicePeriod();
        WeakReference<ProgressBar> getProgressBar();
        void setProgressValue(int position, float value);
        void setProgressBarAnimThread(Thread thread);
        void handeProgressBarAnimTask(int state);

    }

    // Constructor
    ProgressBarAnimRunnable(ProgressBarAnimMethods callback) {
        mCallback = callback;
    }

    @Override
    public void run() {
        mCallback.setProgressBarAnimThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        int pos = mCallback.getServicePosition();
        int period = mCallback.getServicePeriod();

        ProgressBarAnimation anim = new ProgressBarAnimation(0, period);
        anim.setDuration(1000);
        float value = anim.getProgressValue();
        log.i("Progress Value: %s", value);
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
