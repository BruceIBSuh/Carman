package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;
import android.util.SparseArray;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;

import java.util.List;

public class UploadBitmapTask extends ThreadTask implements UploadBitmapRunnable.BitmapResizeMethods{

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadBitmapTask.class);

    static final int UPLOAD_BITMAP_COMPLETE = 1;
    static final int UPLOAD_BITMAP_FAIL = -1;

    // Objects
    private Runnable mBitmapResizeRunnable;
    private ImageViewModel viewModel;
    private Uri uriAttached;
    private int position;
    private SparseArray<String> sparseImageArray;


    // Constructor
    UploadBitmapTask(Context context) {
        mBitmapResizeRunnable = new UploadBitmapRunnable(context, this);

        sparseImageArray = new SparseArray<>();
    }

    void initBitmapTask(Uri uri, int position, ImageViewModel viewModel) {
        uriAttached = uri;
        this.position = position;
        this.viewModel = viewModel;
    }

    Runnable getBitmapResizeRunnable() {
        return mBitmapResizeRunnable;
    }


    void recycle(){
        //srcList = null;
    }

    @Override
    public Uri getAttachedImageUri() {
        return uriAttached;
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
    public void setDownloadBitmapUri(String uri, int key) {

        // Create SparseArray with the uri downloaded from Storage as the value and the position
        // that the attached image is located as the key for purpose of put images in sequentially
        // right position in the content when reading the posting content
        log.i("download image: %s %s", uri, position);
        sparseImageArray.put(key, uri);
        viewModel.getDownloadBitmapUri().postValue(sparseImageArray);
    }

    @Override
    public void handleUploadBitmapState(int state) {
        int outState = -1;
        switch(state) {
            case UPLOAD_BITMAP_COMPLETE:
                outState = ThreadManager.UPLOAD_BITMAP_COMPLETED;
                break;

            case UPLOAD_BITMAP_FAIL:
                outState = ThreadManager.DOWNLOAD_CURRENT_STATION_FAILED;
                break;
        }

        sThreadManager.handleState(this, outState);
    }
}
