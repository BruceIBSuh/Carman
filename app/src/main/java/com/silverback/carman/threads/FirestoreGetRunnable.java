package com.silverback.carman.threads;

import android.os.Process;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.Opinet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreGetRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FirestoreGetRunnable.class);

    // Objects
    private final FireStoreGetMethods mCallback;
    private FirebaseFirestore firestore;
    private List<Opinet.GasStnParcelable> stnList;

    // Interface
    public interface FireStoreGetMethods {
        List<Opinet.GasStnParcelable> getStationList();
        void setStationTaskThread(Thread thread);
        void setStationId(String stnId);
        void setCarWashInfo(int position, boolean isCarwash);
        void handleTaskState(int state);
    }

    // Constructor
    FirestoreGetRunnable(FireStoreGetMethods callback) {
        this.mCallback = callback;
        if(firestore == null) firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void run() {
        mCallback.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        stnList = mCallback.getStationList();
        try {
            if(Thread.interrupted()) throw new InterruptedException();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Bugs have occurred many times here. NullPointerException is brought about due to
        for(int i = 0; i < stnList.size(); i++) {
            final int pos = i;
            String stnId = stnList.get(pos).getStnId();
            final DocumentReference docRef = firestore.collection("gas_station").document(stnId);
            /* Process for adding new gas stations if no documents exists in FireStore, initiating
             * FirestoreSetRunnable with a station id passed which calls Opinet.GasStationInfo
             * to add additional info to FireStore. SnapshotListener notifies the task of being
             * ready to read newly added fields including the field of "carwash".
             * On the other hand, if any document exists, directly read "carwash" field.
             */
            docRef.addSnapshotListener((snapshot, e) -> {
                if(e != null) return;
                // Bugs frequently occurred here maybe b/c snapshot.get("carwash") would sometimes
                // result in null value. Bugs may be fixed by the following way.
                // If snapshot data is null, make it false to get it countered in SparseBooleanArray
                // to notify when it should send the notification to end up the array.
                if(snapshot != null && snapshot.exists()){
                    if(snapshot.get("carwash") != null) {
                        log.i("car wash:%s", snapshot.get("carwash"));
                        Boolean hasCarwash = snapshot.getBoolean("carwash");
                        if(hasCarwash != null) mCallback.setCarWashInfo(pos, hasCarwash);
                        //mCallback.handleTaskState(StationListTask.FIRESTORE_SET_COMPLETE);
                    } else {
                        mCallback.setCarWashInfo(pos, false); //to match the number w/ the station list.
                        setStationData(pos);
                    }
                } else setStationData(pos);
            });
        }
    }

    // Upload data of a station which have not been uploaded to Firestore before, notifying
    // FirestoreSetRunnable of station ids to have additional information from Opinet and upload
    // it to Firestore.
    private void setStationData(final int position) {
        final String stnId = stnList.get(position).getStnId();
        Map<String, Object> stnData = new HashMap<>();
        stnData.put("stn_name", stnList.get(position).getStnName());
        stnData.put("stn_code", stnList.get(position).getStnCode());
        stnData.put("katec_x", stnList.get(position).getLongitude());
        stnData.put("katec_y", stnList.get(position).getLatitude());

        firestore.collection("gas_station").document(stnId)
                .set(stnData)
                .addOnSuccessListener(documentReference -> {
                    mCallback.setStationId(stnList.get(position).getStnId());
                    mCallback.handleTaskState(StationListTask.FIRESTORE_GET_COMPLETE);
                }).addOnFailureListener(error -> log.e("failed to add data"));
    }

}
