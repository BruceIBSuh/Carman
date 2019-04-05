package com.silverback.carman2.threads;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Process;

import com.ibnco.carman.convertgeocoords.GeoPoint;
import com.ibnco.carman.convertgeocoords.GeoTrans;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.XmlPullParserHandler;
import com.silverback.carman2.views.StationRecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class StationListRunnable implements Runnable{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListRunnable.class);

    // Constants
    private static final String OPINET = "http://www.opinet.co.kr/api/aroundAll.do?code=F186170711&out=xml";

    static final int DOWNLOAD_NEAR_STATIONS_COMPLETE = 1;
    static final int DOWNLAOD_CURRENT_STATION_COMPLETE = 2;
    static final int DOWNLOAD_NEAR_STATIONS_FAILED = -1;
    static final int DOWNLOAD_CURRENT_STATION_FAILED = -2;
    static final int DOWNLOAD_NO_STATION = -3;

    // Objects
    private Context context;
    private List<Opinet.GasStnParcelable> mStationList;
    private StationListMethod mTask;
    private URL url;


    // Interface
    public interface StationListMethod {
        String[] getDefaultParam();
        Location getStationLocation();
        void setStationTaskThread(Thread thread);
        void setStationList(List<Opinet.GasStnParcelable> list);
        void handleStationTaskState(int state);
    }

    // Constructor
    StationListRunnable(Context context, StationListMethod task) {
        mStationList = null;
        this.context = context;
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

        log.i("Default Params: %s, %s, %s", fuelCode, radius, sort);

        // Convert longitute and latitude-based location to TM(Transverse Mercator), then again to
        // Katec location using convertgeocoords which is distributed over internet^^ tnx.
        GeoPoint in_pt = new GeoPoint(location.getLongitude(), location.getLatitude());
        GeoPoint tm_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.TM, in_pt);
        GeoPoint katec_pt = GeoTrans.convert(GeoTrans.TM, GeoTrans.KATEC, tm_pt);
        float x = (float) katec_pt.getX();
        float y = (float) katec_pt.getY();


        // Complete the OPINET_ARUND URL w/ the given requests
        final String OPINET_AROUND = OPINET
                + "&x=" + String.valueOf(x)
                + "&y=" + String.valueOf(y)
                + "&radius=" + radius
                + "&sort=" + sort // 1: price 2: distance
                + "&prodcd=" + fuelCode;


        XmlPullParserHandler xmlHandler = new XmlPullParserHandler();
        HttpURLConnection conn = null;
        InputStream is = null;

        try {

            if(Thread.interrupted()) throw new InterruptedException();

            // Option: url.openStream()
            /*
            final URL url = new URL(OPINET_AROUND);
            url.openConnection().setConnectTimeout(5000);
            url.openConnection().setReadTimeout(5000);
            is = url.openStream();
            */
            // Option: HttpURLConnection.getInputStream()
            final URL url = new URL(OPINET_AROUND);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            //bis = new BufferedInputStream(conn.getInputStream());
            is = conn.getInputStream();
            log.i("InputStream: %s", is);

            mStationList = xmlHandler.parseStationListParcelable(is);
            log.i("StationListRunnable:%s", mStationList.size());

            // Fetch the current station which is located within MIN_RADIUS. This is invoked from
            // GasManagerActivity
            if(mStationList.size() > 0) {
                log.i("StationListRunnable");
                if(radius.matches(Constants.MIN_RADIUS)) {
                    log.i("Current Station: %s", mStationList.get(0).getStnName());
                    mTask.setStationList(mStationList);
                    mTask.handleStationTaskState(DOWNLAOD_CURRENT_STATION_COMPLETE);

                } else {
                    log.i("StationList: %s", mStationList.size());
                    Uri uri = saveNearStationInfo(mStationList);
                    if (uri != null) {
                        mTask.setStationList(mStationList);
                        mTask.handleStationTaskState(DOWNLOAD_NEAR_STATIONS_COMPLETE);
                    }
                }

            } else {
                if(radius.matches(Constants.MIN_RADIUS)) mTask.handleStationTaskState(DOWNLOAD_CURRENT_STATION_FAILED);
                else mTask.handleStationTaskState(DOWNLOAD_NEAR_STATIONS_FAILED);
            }

        } catch (MalformedURLException e) {
            log.e("MalformedURLException: %s", e.getMessage());
            mTask.handleStationTaskState(DOWNLOAD_NEAR_STATIONS_FAILED);

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
            mTask.handleStationTaskState(DOWNLOAD_NEAR_STATIONS_FAILED);

        } catch (InterruptedException e) {
            log.e("InterruptedException: %s", e.getMessage());
            mTask.handleStationTaskState(DOWNLOAD_NEAR_STATIONS_FAILED);

        } finally {

            try {
                if(is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(conn != null) conn.disconnect();
        }
    }

    // Save the downloaded near station list in the designated file location.
    private Uri saveNearStationInfo(List<Opinet.GasStnParcelable> list) {

        File file = new File(context.getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);

        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);

            return Uri.fromFile(file);

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        }

        return null;
    }

}
