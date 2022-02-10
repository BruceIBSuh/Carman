package com.silverback.carman.threads;

import android.content.Context;
import android.net.Uri;
import android.util.SparseArray;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.ImageViewModel;

import java.lang.ref.WeakReference;

public class UploadBitmapTask extends ThreadTask implements UploadBitmapRunnable.BitmapResizeMethods{

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadBitmapTask.class);

    // Objects
    private WeakReference<ImageViewModel> weakReference;
    private final Runnable mBitmapUploadRunnable;
    private final SparseArray<Uri> sparseImageArray;
    private Uri imageUri;
    private int position;

    // Constructor
    UploadBitmapTask(Context context) {
        mBitmapUploadRunnable = new UploadBitmapRunnable(context, this);
        sparseImageArray = new SparseArray<>();
    }

    void initBitmapTask(Uri uri, int position, ImageViewModel model) {
        imageUri = uri;
        this.position = position;
        weakReference = new WeakReference<>(model);
        //this.model = model;
    }

    Runnable getBitmapUploadRunnable() {
        return mBitmapUploadRunnable;
    }

    @Override
    public Uri getAttachedImageUri() {
        return imageUri;
    }

    @Override
    public int getImagePosition() {
        return position;
    }

    @Override
    public void setBitmapTaskThread(Thread thread) {
        setCurrentThread(thread);
    }


    @Override
    public void setDownloadBitmapUri(final int key, final Uri uri) {
        sparseImageArray.put(key, uri);
        if(weakReference != null) {
            weakReference.get().getDownloadBitmapUri().postValue(sparseImageArray);
        }
    }

    @Override
    public void handleUploadBitmapState(int state) {
        int outState = -1;
        switch(state) {
            case UploadBitmapRunnable.UPLOAD_BITMAP_COMPLETE:
                outState = sThreadManager.TASK_COMPLETE;
                break;

            case UploadBitmapRunnable.UPLOAD_BITMAP_FAIL:
                outState = sThreadManager.TASK_FAIL;
                break;
        }

        handleTaskState(this, outState);
    }

    public void recycle(){
        sparseImageArray.clear();
        if(weakReference != null) {
            weakReference.clear();
            weakReference = null;
        }

    }
}
