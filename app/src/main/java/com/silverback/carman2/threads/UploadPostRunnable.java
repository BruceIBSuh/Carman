package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;
import java.util.Map;

public class UploadPostRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadPostRunnable.class);

    // Objects
    private Context mContext;
    private UploadPostMethods mTask;
    private FirebaseFirestore firestore;


    public interface UploadPostMethods {
        List<Uri> getImageUriList();
        Map<String, Object> getFirestorePost();
        String getPostContent();
        void setUploadPostThread(Thread thread);
    }


    UploadPostRunnable(Context context, UploadPostMethods task){
        mContext = context;
        mTask = task;
        firestore = FirebaseFirestore.getInstance();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTask.setUploadPostThread(Thread.currentThread());

        String contentBody = mTask.getPostContent();

        // Convert the image uri list to String array for Arrays.asList()
        List<Uri> uriList = mTask.getImageUriList();
        String[] arrImageUri = new String[uriList.size()];
        for (int i = 0; i < uriList.size(); i++) arrImageUri[i] = uriList.get(i).toString();


        // Query the user data with the retrieved user id.
        Map<String, Object> firePost = mTask.getFirestorePost();
        final String userId = (String) firePost.get("user_id");


        // Retrieve the user name and user pic with the user Id and contain them in the Map, then
        // upload the Map to Firestore
        firestore.collection("users").document(userId).get().addOnSuccessListener(document -> {
            String userName = document.getString("user_name");
            String userPic = document.getString("user_pic");
            if (!userName.isEmpty()) firePost.put("user_name", userName);
            if (!userPic.isEmpty()) firePost.put("user_pic", userPic);

            // Upload the post along with the queried user data, which may prevent latency to load
            // the user data if the post retrieves the user data from different collection.
            firestore.collection("board_general").add(firePost)
                    .addOnSuccessListener(docref -> {
                        // Notify BoardPagerFragment of completing upload to upadte the fragment.

                    })
                    .addOnFailureListener(e -> log.e("upload failed: %s", e.getMessage()));
        });



    }
}
