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
    private SparseArray<Uri> sparseImageArray;
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
    public void setDownloadBitmapUri(int key, Uri uri) {
        // Create SparseArray with the uri downloaded from Storage as the value and the position
        // that the attached image is located as the key for purpose of put images in sequentially
        // right position in the content when reading the posting content
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
                outState = ThreadManager2.TASK_COMPLETE;
                break;

            case UploadBitmapRunnable.UPLOAD_BITMAP_FAIL:
                outState = ThreadManager2.TASK_FAIL;
                break;
        }

        handleTaskState(this, outState);
    }

    public void recycle(){
        if(weakReference != null) {
            weakReference.clear();
            weakReference = null;
        }
        /*
        if(sparseImageArray != null) {
            sparseImageArray.clear();
            sparseImageArray = null;
        }

         */

    }
}
