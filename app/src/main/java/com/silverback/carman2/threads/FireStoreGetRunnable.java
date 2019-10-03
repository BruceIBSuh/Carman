package com.silverback.carman2.threads;

import android.os.Process;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ibnco.carman.convertgeocoords.GeoPoint;
import com.ibnco.carman.convertgeocoords.GeoTrans;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireStoreGetRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreGetRunnable.class);


    // Objects
    private FireStoreGetMethods mCallback;
    private FirebaseFirestore firestore;
    private List<Opinet.GasStnParcelable> stnList;


    public interface FireStoreGetMethods {
        List<Opinet.GasStnParcelable> getStationList();
        void setStationTaskThread(Thread thread);
        void setStationId(String stnId);
        void setCarWashInfo(int position, boolean isCarwash);
        void handleStationTaskState(int state);
    }

    FireStoreGetRunnable(FireStoreGetMethods callback) {
        this.mCallback = callback;
        if(firestore == null) firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void run() {
        mCallback.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        stnList = mCallback.getStationList();
        log.i("Station List: %s", stnList.size());

        // Bugs have occurred many times here. NullPointerException is brought about due to
        //for(Opinet.GasStnParcelable stn : stnList) {
        for(int i = 0; i < stnList.size(); i++) {
            //synchronized(this) {
                final int pos = i;
                final String stnId = stnList.get(pos).getStnId();
                final DocumentReference docRef = firestore.collection("gas_station").document(stnId);

                docRef.addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        log.e("SnapshotListener failed: %s", e.getMessage());
                        return;
                    }

                    String source = (snapshot != null && snapshot.getMetadata().hasPendingWrites())?
                            "Local" : "Server";
                    log.i("source: %s", source);

                    /*
                     * Process to add new gas stations if no documents exists in FireStore, initiating
                     * FireStoreSetRunnable with a station id passed which calls Opinet.GasStationInfo
                     * to add additional info to FireStore and notifies the listener ready to read
                     * newly added fields including the field of "carwash".
                     * On the other hand, if any document exists, directly read "carwash" field.
                     */

                    // Bugs frequently occurred here maybe b/c snapshot.get("carwash") would sometimes
                    // result in null value. Bugs may be fixed by the following way.
                    // If snapshot data is null, make it false to get it countered in SparseBooleanArray
                    // to notify when it should send the notification to end up the array.
                    if (snapshot != null && snapshot.exists()){
                        if(snapshot.get("carwash") != null) {
                            mCallback.setCarWashInfo(pos, (boolean) snapshot.get("carwash"));
                        } else {
                            log.e("carwash value is null:%s", snapshot.getString("stnName"));
                            mCallback.setCarWashInfo(pos, false);
                            // Error occurred here. Indefinite looping and required to switch set to update.
                            setStationData(pos);
                        }

                    } else {

                        setStationData(pos);
                    }
                });
            //}
        }
    }


    private void setStationData(final int position) {
        Map<String, Object> stnData = new HashMap<>();
        stnData.put("stn_name", stnList.get(position).getStnName());
        stnData.put("stn_code", stnList.get(position).getStnCode());
        stnData.put("katec_x", stnList.get(position).getLongitude());
        stnData.put("katec_y", stnList.get(position).getLatitude());

        firestore.collection("gas_station").document(stnList.get(position).getStnId()).set(stnData)
                .addOnSuccessListener(documentReference -> {
                    log.i("successfully added data");
                    // Start the task to retrieve addtional information
                    mCallback.setStationId(stnList.get(position).getStnId());
                    mCallback.handleStationTaskState(StationListTask.FIRESTORE_GET_COMPLETE);
                })
                .addOnFailureListener(error -> log.e("failed to add data"));
    }

}
