package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;

public class BitmapResizeTask extends ThreadTask implements BitmapResizeRunnable.BitmapResizeMethods{

    // Objects
    private Context context;
    private Runnable mBitmapResizeRunnable;
    private Uri uri;

    // Constructor
    BitmapResizeTask(Context context) {
        this.context = context;
        mBitmapResizeRunnable = new BitmapResizeRunnable(context, this);
    }

    void initBitmapTask(Uri uri) {
        this.uri = uri;
    }

    Runnable getmBitmapResizeRunnable() {
        return mBitmapResizeRunnable;
    }

    @Override
    public Uri getImageUri() {
        return uri;
    }

    @Override
    public void setBitmapTaskThread(Thread thread) {
        setCurrentThread(thread);
    }
}
