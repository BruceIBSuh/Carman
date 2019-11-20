package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;

public class UploadBitmapTask extends ThreadTask implements UploadBitmapRunnable.BitmapResizeMethods{

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadBitmapTask.class);

    // Objects
    private Context context;
    private Runnable mBitmapResizeRunnable;
    private Uri srcUri, uploadUri;
    private ImageViewModel bitmapModel;

    // Constructor
    UploadBitmapTask(Context context) {
        this.context = context;
        mBitmapResizeRunnable = new UploadBitmapRunnable(context, this);
    }

    void initBitmapTask(Uri uri, ImageViewModel model) {
        srcUri = uri;
        bitmapModel = model;
    }

    Runnable getmBitmapResizeRunnable() {
        return mBitmapResizeRunnable;
    }

    @Override
    public Uri getImageUri() {
        return srcUri;
    }

    @Override
    public void setBitmapTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setBitmapUri(Uri uri) {
        log.i("uploadUri: %s", uri);
        bitmapModel.getUploadBitmap().postValue(uri);
    }
}
