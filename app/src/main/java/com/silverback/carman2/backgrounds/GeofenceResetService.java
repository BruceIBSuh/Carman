package com.silverback.carman2.backgrounds;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class GeofenceResetService extends IntentService {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceResetService.class);


    // Objects
    private FirebaseFirestore firestore;

    // Constructor
    public GeofenceResetService() {
        super("GeofenceResetService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try (FileInputStream fis = openFileInput("user_id");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

            final String userId = br.readLine();
            firestore = FirebaseFirestore.getInstance();
            firestore.collection("users").document(userId).collection("geofence").get()
                    .addOnSuccessListener(snapshot -> {
                        for(DocumentSnapshot document : snapshot) {
                            log.i("Geofence: %s", document);


                        }
                    });

        } catch(IOException e) {
            log.e("IOException when retrieving user id: %s", e.getMessage());
        }


    }
}