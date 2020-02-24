package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;
import java.util.Map;

public class UploadPostRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadPostRunnable.class);

    // Objects
    //private Context mContext;
    private UploadPostMethods mTask;
    private FirebaseFirestore firestore;
    private String postId;


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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTask.setUploadPostThread(Thread.currentThread());

        // Query the user data with the retrieved user id.
        Map<String, Object> post = mTask.getFirestorePost();
        final String userId = (String) post.get("user_id");
        log.i("User ID: %s", userId);

        // Retrieve the user name and pic based on the Id and contain them in the Map
        firestore.collection("users").document(userId).get().addOnSuccessListener(document -> {
            String userName = document.getString("user_name");
            String userPic = document.getString("user_pic");
            if (!userName.isEmpty()) post.put("user_name", userName);
            if (!userPic.isEmpty()) post.put("user_pic", userPic);

            // Upload the post along with the queried user data, which may prevent latency to load
            // the user data if the post retrieves the user data from different collection.
            firestore.collection("board_general").add(post)
                    .addOnSuccessListener(docref -> {
                        log.i("Uploade completed");
                        mTask.notifyUploadDone(docref.getId());
                    })
                    .addOnFailureListener(e -> log.e("Upload failed: %s"));

                    /*
                    .continueWith(task -> {
                        if(!task.isSuccessful()) task.getException();
                        return postId;
                    })
                    .addOnCompleteListener(task -> {
                        log.i("Post Id: %s", postId);
                        firestore.collection("board_general").document(postId).collection("post_content").add(postContent)
                                .addOnSuccessListener(content -> {
                                    log.i("Post content successfully uploaded to post_content collection");
                                })
                                .addOnFailureListener(e -> log.e("Post content failed to upload"));
                    })
                    .addOnFailureListener(e -> log.e("Uploading post failed: %s", e.getMessage()));
                    */

        });

    }

}
