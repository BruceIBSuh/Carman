package com.silverback.carman2.threads;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Process;

import com.ibnco.carman.convertgeocoords.GeoPoint;
import com.ibnco.carman.convertgeocoords.GeoTrans;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.XmlPullParserHandler;

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

public class StationDownloadRunnable implements Runnable{

    // Constants
    //private static final String TAG = "StationDownloadRunnable";
    private static final String OPINET = "http://www.opinet.co.kr/api/aroundAll.do?code=F186170711&out=xml";

    static final int DOWNLOAD_STATION_LIST_COMPLETE = 1;
    static final int DOWNLOAD_CURRENT_STATION_COMPLETE = 2;
    static final int DONWLOAD_STATION_LIST_FAIL = -1;
    static final int DOWNLOAD_NO_NEAR_STATION = -2;
    //static final int DOWNLOAD_NO_CURRENT_STATION = -3;

    // Objects
    private Context context;
    private List<Opinet.GasStnParcelable> mStationList;
    private StationDownloadRunnable.StationsDownloadMethod mTask;


    // Interface
    public interface StationsDownloadMethod {
        String[] getDefaultParam();
        Location getStationLocation();
        void setStationList(List<Opinet.GasStnParcelable> list);
        void setDownloadThread(Thread thread);
        void handleDownloadTaskState(int state);
    }

    // Constructor
    StationDownloadRunnable(Context context, StationsDownloadMethod task) {
        //Log.i(TAG, "StationsDowloadRunnable starts");
        mStationList = null;
        this.context = context;
        mTask = task;
    }

    @Override
    public void run() {

        mTask.setDownloadThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        //Log.d(TAG, "Thread: " + Thread.currentThread());

        String[] defaultParams = mTask.getDefaultParam();
        Location location = mTask.getStationLocation();

        // Get the default params and location passed over here from MainActivity
        String fuelCode = defaultParams[0];
        String radius = defaultParams[1];
        String sort = defaultParams[2];

        //Log.i(TAG, "Default Params: " + fuelCode + ", " + radius + ", " + sort);

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
        //BufferedInputStream bis = null;

        try {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }

            // Option: url.openStream()
            /*
            final URL url = new URL(OPINET_AROUND);
            //url.openConnection().setConnectTimeout(5000);
            //url.openConnection().setReadTimeout(5000);
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

            mStationList = xmlHandler.parseStationListParcelable(is);

            // Fetch the current station which is located within MIN_RADIUS. This is invoked from
            // GasManagerActivity
            if(mStationList.size() > 0) {

                if(radius.matches(Constants.MIN_RADIUS)) {
                    //Log.i(TAG, "Current Station: " + mStationList.get(0).getStnName());
                    mTask.setStationList(mStationList);
                    mTask.handleDownloadTaskState(DOWNLOAD_CURRENT_STATION_COMPLETE);

                } else {
                    //Log.i(TAG, "StationList: " + mStationList.size());
                    Uri uri = saveNearStationInfo(mStationList);
                    if (uri != null) {
                        mTask.setStationList(mStationList);
                        mTask.handleDownloadTaskState(DOWNLOAD_STATION_LIST_COMPLETE);
                    }
                }

            } else {
                if(radius.matches(Constants.MIN_RADIUS)) mTask.handleDownloadTaskState(DONWLOAD_STATION_LIST_FAIL);
                else mTask.handleDownloadTaskState(DOWNLOAD_NO_NEAR_STATION);
            }

        } catch (MalformedURLException e) {
            //Log.e(TAG, "MalformedURLException: " + e.getMessage());
            mTask.handleDownloadTaskState(DONWLOAD_STATION_LIST_FAIL);

        } catch (IOException e) {
            //Log.e(TAG, "IOException: " + e.getMessage());
            mTask.handleDownloadTaskState(DONWLOAD_STATION_LIST_FAIL);

        } catch (InterruptedException e) {
            //Log.e(TAG, "InterruptedException: " + e.getMessage());
            mTask.handleDownloadTaskState(DONWLOAD_STATION_LIST_FAIL);

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

        //File file = new File(getApplicationContext().getFilesDir(), "tmpStationListUri");
        File file = new File(
                context.getApplicationContext().getCacheDir(), Constants.FILE_CACHED_STATION_AROUND);

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(list);

        } catch (FileNotFoundException e) {
            //Log.e(TAG, "FileNotFoundException: " + e.getMessage());

        } catch (IOException e) {
            //Log.e(TAG, "IOException: " + e.getMessage());

        }

        try {
            if (oos != null) oos.close();
            if (fos != null) fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(file);
    }
}
