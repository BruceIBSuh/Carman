package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;

import java.util.Map;

public class UploadPostTask extends ThreadTask implements UploadPostRunnable.UploadPostMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadPostTask.class);

    // Objects
    private Runnable mUploadPostRunnable;
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
    public void notifyUploadDone(String id) {
        log.i("notifyUploadDone: %s", id);
        viewModel.getNewPosting().postValue(id);
    }
}