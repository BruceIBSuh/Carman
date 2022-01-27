package com.silverback.carman.threads;

import android.content.Context;
import android.net.Uri;

import java.util.List;
import java.util.Map;

public class UpdatePostTask extends ThreadTask implements UpdatePostRunnable.UploadPostCallback {

    private Map<String, Object> post;
    private List<String> removedImages;
    private List<Uri> newImages;
    private final Runnable uploadPostRunnable2;

    public UpdatePostTask(Context context) {
        uploadPostRunnable2 = new UpdatePostRunnable(context, this);
    }

    public void initTask(Map<String, Object> post, List<String> removedImages, List<Uri> newImages){
        this.post = post;
        this.removedImages = removedImages;
        this.newImages = newImages;
    }

    Runnable getUploadPostRunnable2() {
        return uploadPostRunnable2;
    }

    @Override
    public void setUploadPostThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public List<String> getRemovedImages() {
        return removedImages;
    }

    @Override
    public List<Uri> getNewImages() {
        return newImages;
    }

    @Override
    public Map<String, Object> getUpdatePost() {
        return this.post;
    }
}


