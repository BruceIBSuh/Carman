package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Process;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

public class DownloadBitmapRunnable implements Runnable {

    // Objects
    private Context context;
    private DownloadBitmapMethods mTask;


    // Interface
    public interface DownloadBitmapMethods {
        void setDownloadBitmapThread(Thread thread);
        void setBitmapTask(Bitmap bitmap);
        String getBitmapUri();
    }

    public DownloadBitmapRunnable(Context context, DownloadBitmapMethods task) {
        this.context = context;
        mTask = task;
    }


    @Override
    public void run() {

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTask.setDownloadBitmapThread(Thread.currentThread());

        final String uriString = mTask.getBitmapUri();
        List<FutureTarget<Bitmap>> futureBitmapList = new ArrayList<>();
        /*
        FutureTarget<Bitmap> futureBitmap = Glide.with(context.getApplicationContext())
                .asBitmap()
                .load(Uri.parse(uriString))
                .submit();
        */

        Glide.with(context.getApplicationContext()).asBitmap()
                .load(Uri.parse(uriString))
                .into(new CustomTarget<Bitmap>() {

                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

    }
}
