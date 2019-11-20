package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;

import java.util.List;
import java.util.Map;

public class UploadPostTask extends ThreadTask implements UploadPostRunnable.UploadPostMethods {


    private Runnable mUploadPostRunnable;
    private Map<String, Object> post;
    private List<Uri> uriImageList;
    private String content;

    UploadPostTask(Context context) {
        mUploadPostRunnable = new UploadPostRunnable(context, this);
    }


    void initPostTask(Map<String, Object> post, String content, List<Uri> uriImageList) {
       this.post = post;
       this.uriImageList = uriImageList;
       this.content = content;
    }

    Runnable getUploadPostRunnable() {
        return mUploadPostRunnable;
    }

    @Override
    public List<Uri> getImageUriList() {
        return uriImageList;
    }

    @Override
    public Map<String, Object> getFirestorePost() {
        return post;
    }

    @Override
    public String getPostContent() {
        return content;
    }

    @Override
    public void setUploadPostThread(Thread thread) {
        setCurrentThread(thread);
    }
}
