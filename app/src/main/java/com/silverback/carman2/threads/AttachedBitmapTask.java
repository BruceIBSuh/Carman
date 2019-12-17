package com.silverback.carman2.threads;

import android.content.Context;
import android.text.style.ImageSpan;
import android.util.SparseArray;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;

import java.util.ArrayList;
import java.util.List;

public class AttachedBitmapTask extends ThreadTask implements AttachedBitmapRunnable.DownloadBitmapMethods{

    private static final LoggingHelper log = LoggingHelperFactory.create(AttachedBitmapTask.class);

    // Objects
    private Runnable mAttachedBitmapRunnable;
    private String uriString;
    //private List<String> uriStringList;
    private FirestoreViewModel viewModel;
    private List<ImageSpan> imgSpanList;
    private int position;

    AttachedBitmapTask(Context context) {
        log.i("AttachedBitmapTask");
        mAttachedBitmapRunnable = new AttachedBitmapRunnable(context, this);
        imgSpanList = new ArrayList<>();
    }

    //void initTask(List<String> imgUriList, FirestoreViewModel viewModel) {
    void initTask(String uriString, int position, FirestoreViewModel viewModel) {
        //uriStringList = imgUriList;
        this.uriString = uriString;
        this.position = position;
        this.viewModel = viewModel;
    }

    Runnable getAttachedBitmapRunnable() {
        return mAttachedBitmapRunnable;
    }

    void recycle() {
        if(imgSpanList != null) imgSpanList = null;
    }

    @Override
    public void setDownloadBitmapThread(Thread thread) {
        setCurrentThread(thread);
    }


    @Override
    public void setImageSpanArray(SparseArray<ImageSpan> spanArray) {
        viewModel.getAttachedImageSpanList().postValue(spanArray);
    }

    @Override
    public String getImageUriString() {
        return uriString;
    }

    @Override
    public int getImagePosition() {
        return position;
    }

    /*
    @Override
    public List<String> getImageUriList() {
        return uriStringList;
    }

     */

}
