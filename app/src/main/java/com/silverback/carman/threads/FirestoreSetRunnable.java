package com.silverback.carman.threads;

import android.os.Process;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.XmlPullParserHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FirestoreSetRunnable implements Runnable {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(FirestoreSetRunnable.class);
    private static final String OPINET = "https://www.opinet.co.kr/api/detailById.do?code=F186170711&out=xml";

    // Objects
    private final FireStoreSetMethods mCallback;
    private FirebaseFirestore fireStore;
    private final XmlPullParserHandler xmlHandler;

    // Interface
    public interface FireStoreSetMethods {
        void setStationTaskThread(Thread thread);
        void handleTaskState(int state);
        String getStationId();
    }

    // Constructor
    FirestoreSetRunnable(FireStoreSetMethods task) {
        this.mCallback = task;
        if(fireStore == null) fireStore = FirebaseFirestore.getInstance();
        xmlHandler = new XmlPullParserHandler();
    }

    @Override
    public void run() {
        mCallback.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        log.i("FirestoreSetRunnable: %s", Thread.currentThread());

        final String stnId = mCallback.getStationId();
        final String OPINET_DETAIL = OPINET + "&id=" + stnId;
        //HttpURLConnection conn = null;
        //InputStream is = null;
        try {
            if (Thread.interrupted()) throw new InterruptedException();

            URL url = new URL(OPINET_DETAIL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            //conn.connect();
            try (InputStream is = new BufferedInputStream(conn.getInputStream())) {
                Opinet.GasStationInfo stnInfo = xmlHandler.parseGasStationInfo(is);

                boolean isCarwash = stnInfo.getIsCarWash() != null && stnInfo.getIsCarWash().equalsIgnoreCase("Y");
                boolean isService = stnInfo.getIsService() != null && stnInfo.getIsService().equalsIgnoreCase("Y");
                boolean isCVS = stnInfo.getIsCVS() != null && stnInfo.getIsCVS().equalsIgnoreCase("Y");

                // Set additional station info to FireStore using Transaction.
                final DocumentReference docRef = fireStore.collection("gas_station").document(stnId);

                Map<String, Object> data = new HashMap<>();
                data.put("new_addrs", stnInfo.getNewAddrs());
                data.put("old_addrs", stnInfo.getOldAddrs());
                data.put("phone", stnInfo.getTelNo());
                data.put("carwash", isCarwash);
                data.put("service", isService);
                data.put("cvs", isCVS);
                log.i("carwash from StationInfo: %s", isCarwash);

                fireStore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(docRef);
                    if (snapshot.exists()) {
                        transaction.set(docRef, data, SetOptions.merge());
                        mCallback.handleTaskState(GasStationListTask.FIRESTORE_SET_COMPLETE);
                    }

                    return null;
                }).addOnSuccessListener(aVoid -> log.i("Successfully set data to FireStore"))
                        .addOnFailureListener(e -> log.e("Failed to set data to FireStore:%s", e.getMessage()));
            } finally { conn.disconnect(); }

        } catch (IOException | InterruptedException e) {
            log.e("Exception : %s", e.getMessage());
        }
    }
}
