package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Process;

import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ApplyImageResourceUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class DownloadImageRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(DownloadImageRunnable.class);

    // Objects
    private Context context;
    private ApplyImageResourceUtil imgUtil;
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
        void setEmblemUri(Uri uri);
        void handleDownloadImageState(int state);
        String getImageUrl();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        callback.setDownloadImageThread(Thread.currentThread());
        imgUtil = new ApplyImageResourceUtil(context);

        final String imageUrl = callback.getImageUrl();
        final File file = new File(context.getFilesDir() + "/images", "emblem");

        try (InputStream in = new java.net.URL(imageUrl).openStream();
             FileOutputStream fos = new FileOutputStream(file)) {

            Bitmap bitmap = BitmapFactory.decodeStream(in);
            byte[] byteArray = imgUtil.compressBitmap(bitmap, 50000);
            log.i("ByteArray: %s", byteArray.length);
            fos.write(byteArray);

            //RoundedBitmapDrawable roundedBitmap = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
            //roundedBitmap.setCircular(true);

            //callback.setDownloadImage(roundedBitmap);
            //callback.handleDownloadImageState(DownloadImageTask.DOWNLOAD_COMPLETE);

        }catch(Exception e) {
            log.e("Exception: %s", e.getMessage());
            callback.handleDownloadImageState(DownloadImageTask.DOWNLOAD_FAIL);
        }

    }
}
