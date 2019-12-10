package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Process;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class DownloadBitmapRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(DownloadBitmapRunnable.class);

    // Objects
    private Context context;
    private DownloadBitmapMethods mTask;

    // Interface
    public interface DownloadBitmapMethods {
        void setDownloadBitmapThread(Thread thread);
        void setImageSpanArray(SparseArray<ImageSpan> spanArray);
        List<String> getImageUriList();
    }

    // Constructor
    DownloadBitmapRunnable(Context context, DownloadBitmapMethods task) {
        this.context = context;
        mTask = task;
    }


    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTask.setDownloadBitmapThread(Thread.currentThread());

        List<String> uriStringList = mTask.getImageUriList();
        SparseArray<ImageSpan> sparseSpanArray = new SparseArray<>();

        // Calculate the screen size to fit the image to the size by Glide. Not sure why the size
        // should be divided by 2 for fitting.
        Point size = calculateDeviceSize();

        // What if the Glide fails to fetch an image?
        for(int i = 0; i < uriStringList.size(); i++) {
            final int key = i;
            Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(Uri.parse(uriStringList.get(i)))
                    .apply(new RequestOptions().override(size.x / 4, size.y / 4))
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(
                                @NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                            ImageSpan imgSpan = new ImageSpan(context, resource);
                            sparseSpanArray.put(key, imgSpan);

                            if(sparseSpanArray.size() == uriStringList.size()) {
                                mTask.setImageSpanArray(sparseSpanArray);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            log.i("onLoadCleared");
                        }
                    });
        }

    }

    private Point calculateDeviceSize() {
        WindowManager windowManager = (WindowManager)(context.getSystemService(Context.WINDOW_SERVICE));
        Display display = windowManager.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);
        int orientation = display.getRotation();
        log.i("orientation: %s", orientation);

        return size;
    }
}
