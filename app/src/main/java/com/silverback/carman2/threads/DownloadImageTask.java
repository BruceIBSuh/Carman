package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.SparseArray;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;

public class DownloadImageTask extends ThreadTask implements
        DownloadImageRunnable.DownloadImageMethods{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DownloadImageTask.class);

    // Constants
    static final int DOWNLOAD_COMPLETE = 1;
    static final int DOWNLOAD_FAIL = -1;

    // Objects
    private Context context;
    private String imgUrl;
    private ImageViewModel viewModel;
    private Runnable downloadImageRunnable;
    private SparseArray<Bitmap> sparseArray;

    private int position;

    // Constructor
    DownloadImageTask(Context context, ImageViewModel model) {
        this.context = context;
        this.viewModel = model;
        downloadImageRunnable = new DownloadImageRunnable(context, this);
        sparseArray = new SparseArray<>();
        log.d("ViewModel: %s", viewModel);
    }

    void initTask(int position, String url) {
        this.position = position;
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
        viewModel.setDownloadImage(position, drawable);
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
