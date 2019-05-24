package com.silverback.carman2.threads;

import android.os.Process;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.List;

import javax.annotation.Nullable;

public class FireStoreGetRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreGetRunnable.class);


    // Objects
    private FireStoreGetMethods task;
    private FirebaseFirestore mDB;
    private List<Opinet.GasStnParcelable> stnList;


    public interface FireStoreGetMethods {
        void setStationTaskThread(Thread thread);
        List<Opinet.GasStnParcelable> getStationList();
        void handleStationTaskState(int state);

    }

    FireStoreGetRunnable(FireStoreGetMethods task) {
        this.task = task;
        if(mDB == null) mDB = FirebaseFirestore.getInstance();
    }

    @Override
    public void run() {

        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        stnList = task.getStationList();

        for(Opinet.GasStnParcelable station : stnList) {

            Query query = mDB.collection("stations").whereEqualTo("id", station.getStnId());
            query.addSnapshotListener((snapshot, e) -> {

                if(snapshot == null) return;

                if(!snapshot.isEmpty()) {
                    boolean isCarwash = (boolean)snapshot.getDocuments().get(0).get("carwash");
                    station.setIsWash(isCarwash);
                    station.setHasVisited(true);
                } else {
                    station.setHasVisited(false);
                    task.handleStationTaskState(StationListTask.FIRESTORE_GET_COMPLETE);
                }
            });
            /*
            query.addSnapshotListener(new EventListener<QuerySnapshot>(){
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {

                    if(snapshot == null) return;
                    if(!snapshot.isEmpty()) {

                    }

                }
            });
            */
        }

    }
}
