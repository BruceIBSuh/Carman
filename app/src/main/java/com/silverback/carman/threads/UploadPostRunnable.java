package com.silverback.carman.threads;

import android.content.Context;
import android.os.Process;
import android.text.TextUtils;

import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.Map;

public class UploadPostRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadPostRunnable.class);

    // Objects
    //private Context mContext;
    private final UploadPostMethods mTask;
    private final FirebaseFirestore firestore;

    public interface UploadPostMethods {
        Map<String, Object> getFirestorePost();
        void setUploadPostThread(Thread thread);
        void notifyUploadDone(String docId);
    }


    UploadPostRunnable(Context context, UploadPostMethods task){
        //mContext = context;
        mTask = task;
        firestore = FirebaseFirestore.getInstance();

    }

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTask.setUploadPostThread(Thread.currentThread());

        // Query the user data with the retrieved user id.
        Map<String, Object> post = mTask.getFirestorePost();
        final String userId = (String)post.get("user_id");
        log.i("User ID: %s", userId);
        if(userId == null || TextUtils.isEmpty(userId)) return;
        // Retrieve the user name and pic based on the Id and contain them in the Map
        firestore.collection("users").document(userId).get().addOnSuccessListener(document -> {
            if(document.exists()) {
                String userName = document.getString("user_name");
                String userPic = document.getString("user_pic");
                if (!TextUtils.isEmpty(userName)) post.put("user_name", userName);
                if (!TextUtils.isEmpty(userPic)) post.put("user_pic", userPic);
            }
            // Upload the post along with the queried user data, which may prevent latency to load
            // the user data if the post retrieves the user data from different collection.
            firestore.collection("board_general").add(post)
                    .addOnSuccessListener(docref -> {
                        log.i("Uploade completed");
                        mTask.notifyUploadDone(docref.getId());
                    })
                    .addOnFailureListener(e -> log.e("Upload failed: %s"));
        }).addOnFailureListener(aVoid -> {
            log.e("upload failed");
        });

    }

}
