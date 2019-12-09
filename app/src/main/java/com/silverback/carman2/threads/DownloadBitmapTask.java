package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.style.ImageSpan;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;
import com.silverback.carman2.models.ImageViewModel;

import java.util.ArrayList;
import java.util.List;

public class DownloadBitmapTask extends ThreadTask implements DownloadBitmapRunnable.DownloadBitmapMethods{

    private static final LoggingHelper log = LoggingHelperFactory.create(DownloadBitmapTask.class);

    // Objects
    private Runnable downloadBitmapRunnable;
    private List<String> uriStringList;
    private FirestoreViewModel viewModel;
    private List<ImageSpan> imgSpanList;

    // Constructor
    DownloadBitmapTask(Context context) {
        downloadBitmapRunnable = new DownloadBitmapRunnable(context, this);
        imgSpanList = new ArrayList<>();
    }

    void initTask(List<String> imgUriList, FirestoreViewModel viewModel) {
        uriStringList = imgUriList;
        this.viewModel = viewModel;
    }

    Runnable getDownloadBitmapRunnable() {
        return downloadBitmapRunnable;
    }

    @Override
    public void setDownloadBitmapThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void fetchImageSpan(ImageSpan span) {
        imgSpanList.add(span);
        if(imgSpanList.size() == uriStringList.size()) {
            log.i("span: %s", imgSpanList.size());
            viewModel.getAttachedImageSpanList().postValue(imgSpanList);
        }
    }

    @Override
    public List<String> getImageUriList() {
        return uriStringList;
    }
}
