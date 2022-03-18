package com.silverback.carman.threads;

import android.content.Context;
import android.os.Process;
import android.text.TextUtils;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.Map;

public class UploadPostRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadPostRunnable.class);
    static final int UPLOAD_TASK_COMPLETE = 1;
    static final int UPLOAD_TASK_FAIL = -1;

    // Objects
    //private Context mContext;
    private final UploadPostMethods mTask;
    private final FirebaseFirestore firestore;

    public interface UploadPostMethods {
        Map<String, Object> getFirestorePost();
        void setUploadPostThread(Thread thread);
        void notifyUploadPostDone(DocumentReference docref);
        void handleUploadPostState(int state);
    }


    UploadPostRunnable(Context context, UploadPostMethods task){
        //mContext = context;
        mTask = task;
        firestore = FirebaseFirestore.getInstance();

    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTask.setUploadPostThread(Thread.currentThread());
        // Query the user data with the retrieved user id.
        Map<String, Object> post = mTask.getFirestorePost();
        final String userId = (String) post.get("user_id");
        try {
            if (userId == null || TextUtils.isEmpty(userId)) throw new NullPointerException();
        } catch (NullPointerException e) {
            mTask.handleUploadPostState(UPLOAD_TASK_FAIL);
            e.printStackTrace();
            return;
        }

        final DocumentReference docref = firestore.collection("users").document(userId);
        firestore.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot doc = transaction.get(docref);
            if (doc.exists()) {
                post.put("user_name", doc.getString("user_name"));
                post.put("user_pic", doc.getString("user_pic"));
                firestore.collection("user_post").add(post).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        DocumentReference postRef = task.getResult();
                        mTask.notifyUploadPostDone(postRef);
                    }
                });
            }

            return null;
        });
    }
        // Retrieve the user name and pic based on the Id and contain them in the Map
        /*
        firestore.collection("users").document(userId).get().addOnSuccessListener(document -> {
            if(document.exists()) {
                String userName = document.getString("user_name");
                String userPic = document.getString("user_pic");
                if (!TextUtils.isEmpty(userName)) post.put("user_name", userName);
                if (!TextUtils.isEmpty(userPic)) post.put("user_pic", userPic);
            }
            // Upload the post along with the queried user data, which may prevent latency to load
            // the user data if the post retrieves the user data from different collection.
            firestore.collection("user_post").add(post)
                    .addOnSuccessListener(docref -> {
                        mTask.notifyUploadDone(docref.getId());
                        mTask.handleUploadPostState(UPLOAD_TASK_COMPLETE);
                    })
                    .addOnFailureListener(e -> mTask.handleUploadPostState(UPLOAD_TASK_COMPLETE));

        }).addOnFailureListener(aVoid -> log.e("upload failed"));
         */

}
