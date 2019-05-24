package com.silverback.carman2.threads;

import android.graphics.Point;
import android.os.Process;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class FireStoreSetRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreSetRunnable.class);



    // Objects
    private FireStoreSetMethods task;
    private FirebaseFirestore db;
    //private List<Opinet.GasStnParcelable> stnList;
    //private WriteBatch batch;

    // Interface
    public interface FireStoreSetMethods {
        void setStationTaskThread(Thread thread);
        List<Opinet.GasStnParcelable> getStationList();
    }

    // Constructor
    FireStoreSetRunnable(FireStoreSetMethods task) {
        this.task = task;
        if(db == null) db = FirebaseFirestore.getInstance();

    }

    @Override
    public void run() {
        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        //CollectionReference collRef = db.collection("stations");
        //batch = db.batch();
        List<Opinet.GasStnParcelable> stnList = task.getStationList();

        for(final Opinet.GasStnParcelable station : stnList) {
            //batch = db.batch();
            final Map<String, Object> data = new HashMap<>();

            // Check if the station already exists by querying the collection with the station id
            // at first. Undess it exists, set station data in the store to prevent updated fields
            // from being reverted to the default value.
            Query query = db.collection("stations").whereEqualTo("id", station.getStnId());
            /*
            query.addSnapshotListener(new EventListener<QuerySnapshot>(){
                @SuppressWarnings("ConstantConditions")
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {

                    if (snapshot.isEmpty()) {
                        data.put("id", station.getStnId());
                        data.put("name", station.getStnName());
                        data.put("addrs", null);
                        data.put("carwash", false);
                        data.put("cvs", false);
                        data.put("service", false);
                        data.put("tel", null);
                        data.put("geocode", new Point((int) station.getLatitude(), (int) station.getLongitude()));

                        DocumentReference docRef = FirebaseFirestore.getInstance()
                                .collection("stations").document(station.getStnId());
                        docRef.set(data, SetOptions.merge());
                        //batch.set(docRef, data, SetOptions.merge());
                    }
                }
            });
            */
            query.addSnapshotListener((snapshot, e) -> {

                if (snapshot != null && snapshot.isEmpty()) {
                    data.put("id", station.getStnId());
                    data.put("name", station.getStnName());
                    data.put("addrs", "");
                    data.put("carwash", false);
                    data.put("cvs", false);
                    data.put("service", false);
                    data.put("tel", "");
                    data.put("geocode", new Point((int) station.getLatitude(), (int) station.getLongitude()));

                    DocumentReference docRef = db.collection("stations").document(station.getStnId());
                    docRef.set(data, SetOptions.merge());
                    //batch.set(docRef, data, SetOptions.merge());
                }
            });
        }


        /*
        //java.lang.IllegalStateException: A write batch can no longer be used after commit() has been called.
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                log.i("Save the data in Firestore complete");
            }
        });
        */
    }
}
