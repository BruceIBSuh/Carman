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

public class StationListRunnable implements Runnable{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListRunnable.class);

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
        void setStationList(List<Opinet.GasStnParcelable> list);
        void setCurrentStation(Opinet.GasStnParcelable station);
        //void notifyException(String msg);
        void handleStationTaskState(int state);
    }

    // Constructor
    StationListRunnable(StationListMethod task) {
        if(fireStore == null) fireStore = FirebaseFirestore.getInstance();
        mStationList = null;
        mTask = task;
    }

    @Override
    public void run() {
        mTask.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String[] defaultParams = mTask.getDefaultParam();
        Location location = mTask.getStationLocation();

        // Get the default params and location passed over here from MainActivity
        String fuelCode = defaultParams[0];
        String radius = defaultParams[1];
        String sort = defaultParams[2];
        log.i("defaultParams: %s, %s, %s", fuelCode, radius, sort);

        // Convert longitute and latitude-based location to TM(Transverse Mercator), then again to
        // Katec location using convertgeocoords which is distributed over internet^^ tnx.
        GeoPoint in_pt = new GeoPoint(location.getLongitude(), location.getLatitude());
        GeoPoint tm_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.TM, in_pt);
        GeoPoint katec_pt = GeoTrans.convert(GeoTrans.TM, GeoTrans.KATEC, tm_pt);
        float x = (float) katec_pt.getX();
        float y = (float) katec_pt.getY();
        log.i("location x, y: %f %f", x, y);
        // Complete the OPINET_ARUND URL w/ the given requests
        final String OPINET_AROUND = OPINET
                + "&x=" + x
                + "&y=" + y
                + "&radius=" + radius
                + "&sort=" + sort // 1: price 2: distance
                + "&prodcd=" + fuelCode;

        XmlPullParserHandler xmlHandler = new XmlPullParserHandler();
        HttpURLConnection conn = null;
        InputStream is = null;

        try {
            if(Thread.interrupted()) throw new InterruptedException();
            final URL url = new URL(OPINET_AROUND);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            is = new BufferedInputStream(conn.getInputStream());
            mStationList = xmlHandler.parseStationListParcelable(is);
            log.i("mStationList:%d", mStationList.size());
            // Get near stations which may be the current station if MIN_RADIUS is given as param or
            // it should be near stations located within SEARCHING_RADIUS.
            if(mStationList.size() > 0) {
                // Fetch the current station located within the radius
                if(radius.matches(Constants.MIN_RADIUS)) {
                    mTask.setCurrentStation(mStationList.get(0));
                    mTask.handleStationTaskState(StationListTask.DOWNLOAD_CURRENT_STATION_COMPLETE);
                // Fetch near stations within the searching radius.
                } else {
                    mTask.setStationList(mStationList);
                    mTask.handleStationTaskState(StationListTask.DOWNLOAD_NEAR_STATIONS_COMPLETE);
                }
            } else {
                if(radius.matches(Constants.MIN_RADIUS)) {
                    mTask.handleStationTaskState(StationListTask.DOWNLOAD_CURRENT_STATION_FAIL);
                } else mTask.handleStationTaskState(StationListTask.DOWNLOAD_NEAR_STATIONS_FAIL);
            }


        } catch (IOException | InterruptedException e) {
            //mTask.notifyException(e.getMessage());
            mTask.handleStationTaskState(StationListTask.DOWNLOAD_NEAR_STATIONS_FAIL);

        } finally {
            try { if(is != null) is.close(); }
            catch (IOException e) { e.printStackTrace(); }
            if(conn != null) conn.disconnect();
        }
    }

}
