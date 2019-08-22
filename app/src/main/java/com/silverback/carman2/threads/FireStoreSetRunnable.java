package com.silverback.carman2.threads;

import android.os.Process;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.XmlPullParserHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FireStoreSetRunnable implements Runnable {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreSetRunnable.class);
    private static final String OPINET = "http://www.opinet.co.kr/api/detailById.do?code=F186170711&out=xml";


    // Objects
    private FireStoreSetMethods mCallback;
    private FirebaseFirestore fireStore;
    private XmlPullParserHandler xmlHandler;

    // Interface
    public interface FireStoreSetMethods {
        void setStationTaskThread(Thread thread);
        void handleStationTaskState(int state);
        String getStationId();
    }

    // Constructor
    FireStoreSetRunnable(FireStoreSetMethods task) {
        this.mCallback = task;
        if(fireStore == null) fireStore = FirebaseFirestore.getInstance();
        xmlHandler = new XmlPullParserHandler();
    }

    @Override
    public void run() {
        mCallback.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        final String stnId = mCallback.getStationId();
        String OPINET_DETAIL = OPINET + "&id=" + stnId;
        HttpURLConnection conn = null;
        InputStream is = null;

        try {
            URL url = new URL(OPINET_DETAIL);
            conn = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(conn.getInputStream());
            Opinet.GasStationInfo stnInfo = xmlHandler.parseGasStationInfo(is);
            final boolean isCarwash = stnInfo.getIsCarWash().equalsIgnoreCase("Y");
            final boolean isService = stnInfo.getIsService().equalsIgnoreCase("Y");
            final boolean isCVS = stnInfo.getIsCVS().equalsIgnoreCase("Y");

            // Set additional station info to FireStore using Transaction.
            final DocumentReference docRef = fireStore.collection("gas_station").document(stnId);

            Map<String, Object> data = new HashMap<>();
            data.put("new_addrs", stnInfo.getNewAddrs());
            data.put("old_addrs", stnInfo.getOldAddrs());
            data.put("phone", stnInfo.getTelNo());
            data.put("carwash", isCarwash);
            data.put("service", isService);
            data.put("cvs", isCVS);

            fireStore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef);
                if(snapshot.exists()) {
                    transaction.set(docRef, data, SetOptions.merge());
                }

                return null;

            }).addOnSuccessListener(aVoid -> log.i("Successfully set data to FireStore"))
            .addOnFailureListener(e -> log.e("Failed to set data to FireStore:%s", e.getMessage()));

        } catch (MalformedURLException e) {
            log.e("MalformedURLException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) conn.disconnect();
        }
    }

    public class Facility {
        boolean carwash;
        boolean cvs;
        boolean service;

        public Facility(){
            // required to have the default constructor
        }
        public Facility(boolean carwash, boolean service, boolean cvs) {
            this.carwash = carwash;
            this.cvs = cvs;
            this.service = service;
        }

        public boolean isCarwash() {
            return carwash;
        }

        public boolean isCvs() {
            return cvs;
        }

        public boolean isService() {
            return service;
        }


    }
}
