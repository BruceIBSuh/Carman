package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ClockRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ClockRunnable.class);

    // Objects
    private Calendar calendar;
    private SimpleDateFormat sdf;
    private ClockMethods task;

    // Interface
    public interface ClockMethods {
        void setClockTaskThread(Thread thread);
        void setCurrentTime(String time);
        void handleClockTaskState();
    }

    // Constructor
    ClockRunnable(Context context, ClockMethods task) {
        this.task = task;
        calendar = Calendar.getInstance(Locale.getDefault());
        sdf = new SimpleDateFormat(context.getResources().getString(R.string.date_format_1), Locale.getDefault());
    }


    @Override
    public void run() {

        task.setClockTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        while(!Thread.interrupted()) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            String currentTime = sdf.format(calendar.getTime());

            try {
                Thread.sleep(1000); //update every 1 minute
            } catch(InterruptedException e) {
                log.e("InterruptedException: %s", e.getMessage());
            }

            log.i("Current Time: %s", currentTime);
            task.setCurrentTime(currentTime);
            task.handleClockTaskState();

        }

    }
}
