package com.silverback.carman2.backgrounds;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.threads.ThreadManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class GeofenceResetService extends IntentService {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceResetService.class);


    // Objects
    private FirebaseFirestore firestore;
    private PendingIntent geofencePendingIntent;
    // Constructor
    public GeofenceResetService() {
        super("GeofenceResetService");
    }

    // Only for Android 26+ to use startForegroundService(). Not sure it is a usage in the right way
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service.startForeground requires that apps hold the permission with Android P+
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForeground(1, new Notification());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(this);


        try (FileInputStream fis = openFileInput("user_id");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {

            final String userId = br.readLine();
            firestore = FirebaseFirestore.getInstance();
            firestore.collection("users").document(userId).collection("geofence").get()
                    .addOnSuccessListener(snapshot -> {
                        for(DocumentSnapshot document : snapshot) {
                            log.i("Geofences: %s", document.get("geofencing"));
                            String name = document.getString("providerName");
                            String id = document.getId();
                            int category = (int)document.get("category");
                            PendingIntent geoPendingIntent = getGeoPendingIntent(id, name, category);


                            geofencingClient.addGeofences((GeofencingRequest)document.get("geofencing"), geoPendingIntent)
                                    .addOnSuccessListener(task -> log.i("Successfully reset Geofence"))
                                    .addOnFailureListener(e -> log.e("Failed to reset"));

                        }
                    });

        } catch(IOException e) {
            log.e("IOException when retrieving user id: %s", e.getMessage());
        }


    }

    private PendingIntent getGeoPendingIntent(String id, String name, int category) {

        if(geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent geoIntent = new Intent(this, GeofenceTransitionService.class);
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("providerId", id);
        bundle.putInt("category", category);
        geoIntent.putExtras(bundle);
        geofencePendingIntent = PendingIntent.getService(this, 0, geoIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return geofencePendingIntent;

    }


}