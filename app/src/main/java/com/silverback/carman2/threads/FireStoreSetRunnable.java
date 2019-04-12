package com.silverback.carman2.threads;

import android.graphics.Point;
import android.os.Process;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class FireStoreSetRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreSetRunnable.class);

    // Objects
    private FireStoreMethods task;
    private FirebaseFirestore db;
    private List<Opinet.GasStnParcelable> stnList;
    private Map<String, Object> data;

    // Interface
    public interface FireStoreMethods {
        void setStationTaskThread(Thread thread);
        List<Opinet.GasStnParcelable> getStationList();
    }

    // Constructor
    FireStoreSetRunnable(FireStoreMethods task) {
        this.task = task;
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void run() {
        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        //CollectionReference collRef = db.collection("stations");
        final WriteBatch batch = db.batch();
        stnList = task.getStationList();
        data = new HashMap<>();

        for(final Opinet.GasStnParcelable station : stnList) {

            // Check if the station already exists by querying the collection with the station id
            // at first. Undess it exists, set the station data in the store.
            // It prevents updated fields from being reverted to the default value.
            Query query = db.collection("stations").whereEqualTo("id", station.getStnId());
            query.get().addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    log.i("Station needs to be set");
                    data.put("id", station.getStnId());
                    data.put("name", station.getStnName());
                    data.put("addrs", "");
                    data.put("carwash", false);
                    data.put("cvs", false);
                    data.put("service", false);
                    data.put("tel", "");
                    data.put("geocode", new Point((int) station.getLatitude(), (int) station.getLongitude()));

                    DocumentReference docRef = FirebaseFirestore.getInstance()
                            .collection("stations").document(station.getStnId());
                    batch.set(docRef, data);
                }
            });
        }

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                log.i("Save the data in Firestore complete");
            }
        });
    }
}
