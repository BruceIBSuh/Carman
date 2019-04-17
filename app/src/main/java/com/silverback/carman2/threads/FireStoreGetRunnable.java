package com.silverback.carman2.threads;

import android.os.Process;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.models.Opinet;

import java.util.List;

import javax.annotation.Nullable;

public class FireStoreGetRunnable implements Runnable {

    // Objects
    private FireStoreGetMethods task;
    private FirebaseFirestore mDB;
    private List<Opinet.GasStnParcelable> stnList;


    public interface FireStoreGetMethods {
        void setStationTaskThread(Thread thread);
        List<Opinet.GasStnParcelable> getStationList();

    }

    FireStoreGetRunnable(FireStoreGetMethods task) {
        this.task = task;
        mDB = FirebaseFirestore.getInstance();
    }

    @Override
    public void run() {

        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        stnList = task.getStationList();

        for(Opinet.GasStnParcelable station : stnList) {

            Query query = mDB.collection("stations").whereEqualTo("id", station.getStnId());
            query.addSnapshotListener(new EventListener<QuerySnapshot>(){
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {



                }
            });
        }

    }
}
