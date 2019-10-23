package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.io.InputStream;


public class DownloadImageRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(DownloadImageRunnable.class);



    // Objects
    private Context context;
    private DownloadImageMethods callback;

    // Constructor
    DownloadImageRunnable(Context context, DownloadImageMethods callback) {
        this.context = context;
        this.callback = callback;
    }

    // Interface
    public interface DownloadImageMethods {
        void setDownloadImageThread(Thread thread);
        void setDownloadImage(RoundedBitmapDrawable bitmap);
        void handleDownloadImageState(int state);
        String getImageUrl();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        callback.setDownloadImageThread(Thread.currentThread());

        final String imageUrl = callback.getImageUrl();
        log.i("image url: %s", imageUrl);

        try (InputStream in = new java.net.URL(imageUrl).openStream()) {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            RoundedBitmapDrawable roundedBitmap = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
            roundedBitmap.setCircular(true);

            callback.setDownloadImage(roundedBitmap);
            callback.handleDownloadImageState(DownloadImageTask.DOWNLOAD_COMPLETE);

        }catch(Exception e) {
            log.e("Exception: %s", e.getMessage());
            callback.handleDownloadImageState(DownloadImageTask.DOWNLOAD_FAIL);
        }

    }
}
