package com.silverback.carman2.threads;

import android.graphics.Point;
import android.os.Process;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireStoreGetRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreGetRunnable.class);


    // Objects
    private FireStoreGetMethods mCallback;
    private FirebaseFirestore fireStore;
    private List<Opinet.GasStnParcelable> stnList;


    public interface FireStoreGetMethods {
        void setStationTaskThread(Thread thread);
        void setStationId(String stnId);
        void setCarWashInfo(int position, Object obj);
        void handleStationTaskState(int state);
        List<Opinet.GasStnParcelable> getStationList();

    }

    FireStoreGetRunnable(FireStoreGetMethods callback) {
        this.mCallback = callback;
        if(fireStore == null) fireStore = FirebaseFirestore.getInstance();
    }

    @Override
    public void run() {
        mCallback.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        stnList = mCallback.getStationList();

        // Bugs have occurred many times here. NullPointerException is brought about due to
        //for(Opinet.GasStnParcelable stn : stnList) {
        for(int i = 0; i < stnList.size(); i++) {
            synchronized (this) {
                final int pos = i;
                final DocumentReference docRef = fireStore.collection("gas_station").document(stnList.get(pos).getStnId());
                docRef.addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    //String source = (snapshot != null && snapshot.getMetadata().hasPendingWrites())?"Local" : "Server";

                    if (snapshot != null && snapshot.exists()) {
                        log.i("document: %s", snapshot.get("carwash"));
                        mCallback.setCarWashInfo(pos, snapshot.get("carwash"));

                    } else {
                        Map<String, Object> stnData = new HashMap<>();
                        //stnData.put("stnId", stnList.get(pos).getStnId());
                        stnData.put("stnName", stnList.get(pos).getStnName());
                        stnData.put("stnCode", stnList.get(pos).getStnCode());
                        stnData.put("xCoord", stnList.get(pos).getLongitude());
                        stnData.put("yCoord", stnList.get(pos).getLatitude());

                        fireStore.collection("gas_station").document(stnList.get(pos).getStnId()).set(stnData)
                                .addOnSuccessListener(documentReference -> {
                                    log.i("successfully added data");
                                    mCallback.setStationId(stnList.get(pos).getStnId());
                                    mCallback.handleStationTaskState(StationListTask.FIRESTORE_GET_COMPLETE);
                                })
                                .addOnFailureListener(error -> log.e("failed to add data"));
                    }
                });


            }
        }
    }

}
