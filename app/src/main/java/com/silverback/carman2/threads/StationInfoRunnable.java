package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.XmlPullParserHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class StationInfoRunnable implements Runnable {

    // Logging
    static private final LoggingHelper log = LoggingHelperFactory.create(StationInfoRunnable.class);

    // Constants
    private static final String OPINET = "http://www.opinet.co.kr/api/detailById.do?code=F186170711&out=xml";
    static final int STATION_INFO_COMPLETE = 3;
    static final int STATION_INFO_FAIL = -3;

    // Objects
    private Context context;
    Opinet.GasStnParcelable parcelable;
    private StationInfoMethod mTask;
    private XmlPullParserHandler xmlHandler;
    private HttpURLConnection conn;
    private InputStream is;

    // Interface
    public interface StationInfoMethod {

        void setStationTaskThread(Thread thread);
        void handleStationTaskState(int state);
        void addGasStationInfo(Opinet.GasStnParcelable parcelable);
        List<Opinet.GasStnParcelable> getStationList();
    }

    // Constructor
    StationInfoRunnable(StationInfoMethod task) {
        mTask = task;
        xmlHandler = new XmlPullParserHandler();
    }


    @Override
    public void run() {

        mTask.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        //List<Opinet.GasStnParcelable> stationList = mTask.getStationList();
        Opinet.GasStationInfo info;

        for (Opinet.GasStnParcelable station : mTask.getStationList()) {

            String OPINET_INFO = OPINET + "&id=" + station.getStnId();
            try {
                if (Thread.interrupted()) throw new InterruptedException();
                info = addStationInfo(OPINET_INFO);
                station.setIsCarWash(info.getIsCarWash()); // add an additional info to the fecthed stations.
                mTask.addGasStationInfo(station);

                //

            } catch (IOException e) {
                log.e("IOException: %s", e);
                //mTask.handleStationTaskState(STATION_INFO_FAIL);

            } catch (InterruptedException e) {
                log.e("InteruptedException: %s", e);
                //mTask.handleStationTaskState(STATION_INFO_FAIL);
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //if(conn != null) conn.disconnect();
            }
        }

        // Notifies StationTask of having the task done.
        mTask.handleStationTaskState(STATION_INFO_COMPLETE);


    }

    private synchronized Opinet.GasStationInfo addStationInfo(final String opinet) throws IOException {

        URL url = new URL(opinet);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Connection", "close");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();
        is = conn.getInputStream();

        return xmlHandler.parseGasStationInfo(is);
    }
}