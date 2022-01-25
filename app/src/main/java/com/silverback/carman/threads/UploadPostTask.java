package com.silverback.carman.threads;

import android.content.Context;
import android.text.TextUtils;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import java.util.Map;

public class UploadPostTask extends ThreadTask implements UploadPostRunnable.UploadPostMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadPostTask.class);

    // Objects
    private final Runnable mUploadPostRunnable;
    private Map<String, Object> post;
    private FragmentSharedModel viewModel;

    // Constructor
    UploadPostTask(Context context) {
        mUploadPostRunnable = new UploadPostRunnable(context, this);
    }


    void initPostTask(Map<String, Object> post, FragmentSharedModel model) {
       this.post = post;
       this.viewModel = model;
    }

    Runnable getUploadPostRunnable() {
        return mUploadPostRunnable;
    }


    @Override
    public Map<String, Object> getFirestorePost() {
        return post;
    }

    @Override
    public void setUploadPostThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void notifyUploadDone(String documentId) {
        log.i("notifyUploadDone: %s", documentId);
        if(!TextUtils.isEmpty(documentId)) viewModel.getNewPosting().postValue(documentId);
    }
}
