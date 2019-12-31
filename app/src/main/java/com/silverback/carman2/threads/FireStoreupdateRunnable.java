package com.silverback.carman2.threads;

import android.os.Process;
import android.text.TextUtils;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.HashMap;
import java.util.Map;

public class FireStoreupdateRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreupdateRunnable.class);

    // Objects
    private FirebaseFirestore fireStore;
    private FireStoreUpdateMethods mCallback;

    // Interface
    public interface FireStoreUpdateMethods {
        void setStationTaskThread(Thread thread);
        String getStationId();
        Opinet.GasStationInfo getStationInfo();
    }

    // Constructor
    FireStoreupdateRunnable(FireStoreUpdateMethods callback) {
        mCallback = callback;
        fireStore = FirebaseFirestore.getInstance();
    }

    @Override
    public void run() {
        mCallback.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String stationId = mCallback.getStationId();
        final Opinet.GasStationInfo stnInfo = mCallback.getStationInfo();
        final DocumentReference docRef = fireStore.collection("gas_station").document(stationId);

        // TEST CODING to convert String "Y" to boolean true or false as to carwash, service, CVS.
        final boolean isCarwash = stnInfo.getIsCarWash().equalsIgnoreCase("Y");
        final boolean isService = stnInfo.getIsService().equalsIgnoreCase("Y");
        final boolean isCVS = stnInfo.getIsCVS().equalsIgnoreCase("Y");

        log.i("Facilities: %s %s %s: ", isCarwash, isService, isCVS);

        // Update a document only if a document contains the address field as empty, which works
        // as a flag for whether it has been updated.
        docRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if(document == null) return;

                if(TextUtils.isEmpty(document.getString("addrs"))) {
                    log.i("Addess flag is null");
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("addrs", stnInfo.getNewAddrs());
                    updates.put("tel", stnInfo.getTelNo());
                    //updates.put("carwash", stnInfo.getIsCarWash());
                    updates.put("carwash", isCarwash);
                    //updates.put("cvs", stnInfo.getIsCVS());
                    updates.put("cvs", isCVS);
                    //updates.put("service", stnInfo.getIsService());
                    updates.put("service", isService);

                    /*
                    docRef.update(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> mCallback) {
                            log.i("Update completes!");
                        }
                    });
                    */
                    //docRef.update(updates).addOnCompleteListener(update -> log.i("Update complelte"));
                }
            }
        });

        /*
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> mCallback) {
                if(mCallback.isSuccessful()) {
                    DocumentSnapshot document = mCallback.getResult();
                    if(document.exists() && document.get("addrs") == null) {
                        log.i("Addess flag is null");
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("addrs", stnInfo.getNewAddrs());
                        updates.put("tel", stnInfo.getTelNo());
                        //updates.put("carwash", stnInfo.getIsCarWash());
                        updates.put("carwash", isCarwash);
                        //updates.put("cvs", stnInfo.getIsCVS());
                        updates.put("cvs", isCVS);
                        //updates.put("service", stnInfo.getIsService());
                        updates.put("service", isService);

                        docRef.update(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> mCallback) {
                                log.i("Update completes!");
                            }
                        });
                    }
                }

            }
        });

        */
    }



}