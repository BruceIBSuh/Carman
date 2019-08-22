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

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(FireStoreSetRunnable.class);
    private static final String OPINET = "http://www.opinet.co.kr/api/detailById.do?code=F186170711&out=xml";


    // Objects
    private FireStoreSetMethods mCallback;
    private FirebaseFirestore fireStore;
    private XmlPullParserHandler xmlHandler;

    // Interface
    public interface FireStoreSetMethods {
        void setStationTaskThread(Thread thread);
        //List<Opinet.GasStnParcelable> getStationList();
        String getStationId();
        void handleStationTaskState(int state);
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
            Opinet.GasStationInfo info = xmlHandler.parseGasStationInfo(is);
            //info.setStationName(task.getStationName());
            log.i("Station Info: %s", info.getNewAddrs());
            setStationExtraInfo(stnId, info);

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

    private synchronized void setStationExtraInfo(final String stnId, final Opinet.GasStationInfo info) {
        final DocumentReference docRef = fireStore.collection("gas_station").document(stnId);
        fireStore.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(docRef);
            if(doc.exists()) {
                log.i("set extra data: %s", docRef);
                Map<String, Object> data = new HashMap<>();
                data.put("new_addrs", info.getNewAddrs());
                data.put("old_addrs", info.getOldAddrs());
                data.put("phone", info.getTelNo());

                final boolean isCarwash = info.getIsCarWash().equalsIgnoreCase("Y");
                final boolean isService = info.getIsService().equalsIgnoreCase("Y");
                final boolean isCVS = info.getIsCVS().equalsIgnoreCase("Y");
                log.i("Facility: %s, %s, %s", isCarwash, isService, isCVS);
                Map<String, Object> extra = new HashMap<>();
                extra.put("carwash", isCarwash);
                extra.put("service", isService);
                extra.put("cvs", isCVS);

                data.put("facility", extra);


                transaction.set(docRef, data, SetOptions.merge());
            }

            return null;
        });
    }

    /*
    private class StationFacility {
        boolean carwash;
        boolean service;
        boolean cvs;

        public StationFacility(){}

        StationFacility(boolean carwash, boolean service, boolean cvs) {
            this.carwash = carwash;
            this.service = service;
            this.cvs = cvs;
        }

        public boolean isCarwash() {
            return carwash;
        }

        public boolean isService() {
            return service;
        }

        public boolean isCvs() {
            return cvs;
        }
    }
    */
}
