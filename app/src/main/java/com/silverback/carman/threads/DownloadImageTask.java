package com.silverback.carman.threads;

import android.content.Context;
import android.net.Uri;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.ImageViewModel;

public class DownloadImageTask extends ThreadTask implements
        DownloadImageRunnable.DownloadImageMethods{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DownloadImageTask.class);

    // Constants
    static final int DOWNLOAD_COMPLETE = 1;
    static final int DOWNLOAD_FAIL = -1;

    // Objects
    private String imgUrl;
    private ImageViewModel viewModel;
    private Runnable downloadImageRunnable;
    //private SparseArray<Bitmap> sparseArray;

    //private int position;

    // Constructor
    DownloadImageTask(Context context, ImageViewModel model) {
        this.viewModel = model;
        downloadImageRunnable = new DownloadImageRunnable(context, this);
        //sparseArray = new SparseArray<>();
        log.d("ViewModel: %s", viewModel);
    }

    void initTask(String url) {
        this.imgUrl = url;
    }

    Runnable getDownloadImageRunnable() {
        return downloadImageRunnable;
    }

    @Override
    public void setDownloadImageThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setDownloadImage(RoundedBitmapDrawable drawable) {
        //viewModel.setDownloadImage(position, drawable);
    }

    @Override
    public void setEmblemUri(Uri uri) {

    }

    @Override
    public String getImageUrl() {
        return imgUrl;
    }

    @Override
    public void handleDownloadImageState(int state) {
        int outstate = -1;
        switch(state) {
            case DOWNLOAD_COMPLETE:

                break;

            case DOWNLOAD_FAIL:

                break;
        }

        outstate = ThreadManager.DOWNLOAD_IMAGE_FINISH;
        sThreadManager.handleState(this, outstate);

    }


}
