package com.silverback.carman.threads;

import android.content.Context;
import android.text.TextUtils;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import java.lang.ref.WeakReference;
import java.util.Map;

public class UploadPostTask extends ThreadTask implements UploadPostRunnable.UploadPostMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadPostTask.class);

    // Objects
    private final Runnable mUploadPostRunnable;
    private WeakReference<FragmentSharedModel> weakModelRef;
    private FragmentSharedModel model;
    private Map<String, Object> post;

    // Constructor
    public UploadPostTask(Context context) {
        mUploadPostRunnable = new UploadPostRunnable(context, this);
    }

    void initPostTask(Map<String, Object> post, FragmentSharedModel model) {
       this.post = post;
       this.model = model;
       weakModelRef = new WeakReference<>(model);
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
        log.i("notifyUploaddone: %s", documentId);
        if(!TextUtils.isEmpty(documentId)) {
            weakModelRef.get().getNewPosting().postValue(documentId);
            model.getNewPosting().postValue(documentId);
        }
    }

    @Override
    public void handleUploadPostState(int state) {
        int outstate = -1;
        switch(state) {
            case UploadPostRunnable.UPLOAD_TASK_COMPLETE:
                outstate = sThreadManager.TASK_COMPLETE;
                break;
            case UploadPostRunnable.UPLOAD_TASK_FAIL:
                outstate = sThreadManager.TASK_FAIL;
                break;
        }

        handleTaskState(this, outstate);
    }

    public void recycle(){
        if(weakModelRef != null) {
            weakModelRef.clear();
            weakModelRef = null;
        }

        if(post != null) post.clear();
    }
}
