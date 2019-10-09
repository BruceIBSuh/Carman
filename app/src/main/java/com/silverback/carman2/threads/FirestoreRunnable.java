package com.silverback.carman2.threads;

import android.os.Process;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class FirestoreRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(FirestoreRunnable.class);
    static final int TASK_COMPLETE = 1;
    static final int TASK_FAIL = -1;

    // Objects
    private FirestoreMethods mTask;


    // interface
    interface FirestoreMethods {
        List<FavoriteProviderEntity> getFavoriteList();
        void setFirestoreThread(Thread thread);
        void setFavoriteSnapshot(int position, DocumentSnapshot snapshot);
        void handleFavoriteState(int state);

    }

    // Constructor
    public FirestoreRunnable(FirestoreMethods task) {
        mTask = task;
    }

    @Override
    public void run() {
        mTask.setFirestoreThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        final List<FavoriteProviderEntity> favList = mTask.getFavoriteList();
        final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        for(int i = 0; i < favList.size(); i++) {
            final int pos = i;
            final String stnId = favList.get(pos).providerId;
            log.i("StationID: %s", stnId);

            firestore.collection("gas_eval").document(stnId).get().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    DocumentSnapshot snapshot = task.getResult();
                    if(snapshot != null && snapshot.exists()) {
                        mTask.setFavoriteSnapshot(pos, snapshot);
                    } else {
                        log.i("No documents exist");
                    }

                } else {
                    log.e("Task Exception: %s", task.getException());
                }
            });
        }

        mTask.handleFavoriteState(TASK_FAIL);
    }
}
