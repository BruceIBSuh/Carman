package com.silverback.carman.threads;

import android.location.Location;
import android.os.Process;

import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.coords.GeoPoint;
import com.silverback.carman.coords.GeoTrans;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.XmlPullParserHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class StationGasRunnable implements Runnable{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationGasRunnable.class);

    // Constants
    private static final String OPINET = "https://www.opinet.co.kr/api/aroundAll.do?code=F186170711&out=xml";

    // Objects
    private FirebaseFirestore fireStore;
    private List<Opinet.GasStnParcelable> mStationList;
    private final StationListMethod mTask;

    // Interface
    public interface StationListMethod {
        String[] getDefaultParam();
        Location getStationLocation();
        void setStationTaskThread(Thread thread);
        void setNearStationList(List<Opinet.GasStnParcelable> list);
        void setCurrentStation(Opinet.GasStnParcelable station);
        void notifyException(String msg);
        void handleTaskState(int state);
    }

    // Constructor
    public StationGasRunnable(StationListMethod task) {
        if(fireStore == null) fireStore = FirebaseFirestore.getInstance();
        mStationList = null;
        mTask = task;
    }

    @Override
    public void run() {
        mTask.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        log.i("statinlistrunnable thread: %s", Thread.currentThread());

        String[] defaultParams = mTask.getDefaultParam();
        Location location = mTask.getStationLocation();

        // Get the default params and location passed over here from MainActivity
        String fuelCode = defaultParams[0];
        String radius = defaultParams[1];
        String sort = defaultParams[2];

        // Convert longitute and latitude-based location to TM(Transverse Mercator), then again to
        // Katec location using the coords package, which is distributed over internet^^.
        GeoPoint in_pt = new GeoPoint(location.getLongitude(), location.getLatitude());
        GeoPoint tm_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.TM, in_pt);
        GeoPoint katec_pt = GeoTrans.convert(GeoTrans.TM, GeoTrans.KATEC, tm_pt);
        float x = (float) katec_pt.getX();
        float y = (float) katec_pt.getY();

        // Complete the OPINET_ARUND URL w/ the given requests
        final String OPINET_AROUND = OPINET
                + "&x=" + x
                + "&y=" + y
                + "&radius=" + radius
                + "&sort=" + sort // 1: price 2: distance
                + "&prodcd=" + fuelCode;

        try {
            if(Thread.interrupted()) throw new InterruptedException();

            final URL url = new URL(OPINET_AROUND);
            XmlPullParserHandler xmlHandler = new XmlPullParserHandler();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            try(InputStream is = new BufferedInputStream(conn.getInputStream())) {
                mStationList = xmlHandler.parseStationListParcelable(is);
                // Get near stations which may be the current station if MIN_RADIUS is given  or
                // it should be near stations located within SEARCHING_RADIUS.
                if(mStationList.size() > 0) {
                    if(radius.matches(Constants.MIN_RADIUS)) {
                        mTask.setCurrentStation(mStationList.get(0));
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_CURRENT_STATION);
                    } else {
                        mTask.setNearStationList(mStationList);
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_NEAR_STATIONS);
                    }
                } else {
                    if(radius.matches(Constants.MIN_RADIUS)) {
                        mTask.handleTaskState(StationGasTask.DOWNLOAD_CURRENT_STATION_FAIL);
                    } else mTask.handleTaskState(StationGasTask.DOWNLOAD_NEAR_STATIONS_FAIL);
                }
            } finally { conn.disconnect(); }

        } catch (IOException | InterruptedException e) {
            mTask.notifyException(e.getLocalizedMessage());
            mTask.handleTaskState(StationGasTask.DOWNLOAD_NEAR_STATIONS_FAIL);
        }
    }

}