package com.silverback.carman2.backgrounds;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.annotation.Nullable;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.FavoriteProviderEntity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/*
 * Service is the base class for all services using the main thread as default. Creating worker
 * threads, it handles multi requests simultaneously, calling onStartCommand() respectively.
 *
 * Whereas, IntentService is a subclass of Service using a worker thread to handle requests one at a time.
 * Here, the class uses Service to handle multi requests.
 */

/*
public class GeofenceResetService extends Service {

    public static final LoggingHelper log = LoggingHelperFactory.create(GeofenceResetService.class);

    private ServiceHandler serviceHandler;
    private GeofencingClient geofencingClient;
    private CarmanDatabase mDB;
    private List<Geofence> geofenceList;
    private PendingIntent mGeofencePendingIntent;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {

        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Retrieve the favorite list querying FavoriteProviderEntity in the CarmanDatabase Room.
            List<FavoriteProviderEntity> favoriteList = mDB.favoriteModel().loadAllFavoriteProvider();
            for(FavoriteProviderEntity entity : favoriteList) {
                geofenceList.add(new Geofence.Builder()
                        .setRequestId(entity.providerId)
                        .setCircularRegion(entity.latitude, entity.longitude, Constants.GEOFENCE_RADIUS)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                        .build());
            }

            geofencingClient.addGeofences(getGeofencingRequest(geofenceList), getGeofencePendingIntent())
                    .addOnSuccessListener(Void -> log.i("Add geofences successfully"))
                    .addOnFailureListener(e -> log.e("Adding geofences failed"));

            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        // Start up the separate thread running the service. By default, Service uses the main thread.
        // Also, create the thread looper and Handler to do the operation in the worker thread.
        HandlerThread handlerThread = new HandlerThread("GeofenceReset", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        Looper serviceLooper = handlerThread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        // Create GeofencingClient and the geofence list for containing the queried result.
        geofencingClient = LocationServices.getGeofencingClient(this);
        mDB = CarmanDatabase.getDatabaseInstance(this);
        geofenceList = new ArrayList<>();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {}

    // Error occurred: java.lang.IllegalArgumentException: No geofence has been added to this request.
    private GeofencingRequest getGeofencingRequest(List<Geofence> geoList) {
        GeofencingRequest.Builder requestBuilder = new GeofencingRequest.Builder();
        requestBuilder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        requestBuilder.addGeofences(geoList);
        return requestBuilder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if(mGeofencePendingIntent != null) return mGeofencePendingIntent;

        Intent intent = new Intent(this, GeofenceTransitionService.class);
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mGeofencePendingIntent;
    }

}
*/

public class GeofenceResetService extends IntentService {

    private static final LoggingHelper log = LoggingHelperFactory.create(GeofenceResetService.class);


    // Objects
    private List<Geofence> geofenceList;
    private PendingIntent mGeofencePendingIntent;

    @Override
    public void onCreate() {}

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
        CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(this);
        geofenceList = new ArrayList<>();

        List<FavoriteProviderEntity> favoriteList = mDB.favoriteModel().loadAllFavoriteProvider();

        for(FavoriteProviderEntity entity : favoriteList) {
            log.i("Favorite: %s, %s, %s", entity.providerName, entity.category, entity.providerId);
            geofenceList.add(new Geofence.Builder()
                    .setRequestId(entity.providerId)
                    .setCircularRegion(entity.latitude, entity.longitude, Constants.GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setLoiteringDelay(Constants.GEOFENCE_LOITERING_TIME)
                    .build());
        }

        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(Void -> log.i("Add geofences successfully"))
                .addOnFailureListener(e -> log.e("Adding geofences failed"));

    }



    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder requestBuilder = new GeofencingRequest.Builder();
        requestBuilder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        requestBuilder.addGeofences(geofenceList);
        return requestBuilder.build();
    }

    private PendingIntent getGeofencePendingIntent() {

        if(mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent geoIntent = new Intent(this, GeofenceTransitionService.class);
        mGeofencePendingIntent = PendingIntent.getService(this, 0, geoIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mGeofencePendingIntent;

    }
}



