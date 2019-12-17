package com.silverback.carman2.threads;

import android.content.Context;
import android.text.style.ImageSpan;
import android.util.SparseArray;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;

public class AttachedBitmapTask extends ThreadTask implements
        AttachedBitmapRunnable.DownloadBitmapMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(AttachedBitmapTask.class);

    // Objects
    private Context context;
    private String uriString;
    private ImageViewModel viewModel;
    private SparseArray<ImageSpan> sparseArray;
    private int position;
    private int cntImage;

    AttachedBitmapTask(Context context, ImageViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
        sparseArray = new SparseArray<>();
        cntImage = 0;
    }

    synchronized void initTask(String uriString, int position) {
        log.i("uri and position: %s, %s", uriString, position);
        this.uriString = uriString;
        this.position = position;
        cntImage++;
    }

    // Initiate multi Runnables with a single task.
    Runnable getAttachedBitmapRunnable() {
        return new AttachedBitmapRunnable(context, uriString, position, this);
    }

    void recycle() {
        sparseArray.clear();
    }

    @Override
    public void setDownloadBitmapThread(Thread thread) {
        setCurrentThread(thread);
    }


    @Override
    public void setImageSpan(SparseArray<ImageSpan> spanArray) {
        sparseArray.put(spanArray.keyAt(0), spanArray.valueAt(0));
        if(sparseArray.size() == cntImage) viewModel.getImageSpanArray().postValue(sparseArray);
        else {
            // In case all the images attached failed to fetch, notify which image to fail and
            // need to display an error image.
            log.e("Attached images failed to fetch");
        }
    }

    @Override
    public void handleTaskState(int state) {
        int outstate = -1;
        switch(state) {

        }
    }
}
