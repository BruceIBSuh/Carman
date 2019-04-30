package com.silverback.carman2.threads;

import android.os.Process;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class FireStoreUpdateRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreUpdateRunnable.class);

    // Objects
    private FirebaseFirestore db;
    private FireStoreUpdateMethods task;

    // Interface
    public interface FireStoreUpdateMethods {
        void setStationTaskThread(Thread thread);
        String getStationId();
        Opinet.GasStationInfo getStationInfo();
    }

    // Constructor
    FireStoreUpdateRunnable(FireStoreUpdateMethods task) {
        this.task = task;
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void run() {
        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String stationId = task.getStationId();
        final Opinet.GasStationInfo stnInfo = task.getStationInfo();
        final DocumentReference docRef = db.collection("stations").document(stationId);


        // TEST CODING to convert String "Y" to boolean true or false as to carwash, service, CVS.
        final boolean isCarwash = stnInfo.getIsCarWash().equalsIgnoreCase("Y");
        final boolean isService = stnInfo.getIsService().equalsIgnoreCase("Y");
        final boolean isCVS = stnInfo.getIsCVS().equalsIgnoreCase("Y");

        log.i("Facilities: %s %s %s: ", isCarwash, isService, isCVS);


        // Update a document only if a document contains the address field as empty, which works
        // as a flag for whether it has been updated.
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
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
                            public void onComplete(@NonNull Task<Void> task) {
                                log.i("Update completes!");
                            }
                        });
                    }
                }

            }
        });
    }
}
