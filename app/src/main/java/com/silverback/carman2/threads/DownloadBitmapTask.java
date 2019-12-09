package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;

import com.silverback.carman2.models.FirestoreViewModel;
import com.silverback.carman2.models.ImageViewModel;

public class DownloadBitmapTask extends ThreadTask {

    // Objects
    private Context context;
    private Runnable downloadBitmapRunnable;

    // Constructor
    public DownloadBitmapTask(Context context) {
        this.context = context;
        downloadBitmapRunnable = new DownloadBitmapRunnable();
    }

    public void initTask(String uriString, FirestoreViewModel model) {

    }

    public Runnable getDownloadBitmapRunnable() {
        return downloadBitmapRunnable;
    }
}
