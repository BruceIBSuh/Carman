package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

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
import java.util.List;

public class StationInfoRunnable implements Runnable {

    // Logging
    static private final LoggingHelper log = LoggingHelperFactory.create(StationInfoRunnable.class);

    // Constants
    private static final String OPINET = "http://www.opinet.co.kr/api/detailById.do?code=F186170711&out=xml";
    static final int DOWNLOAD_STATION_INFO_COMPLETE = 3;
    static final int DOWNLOAD_STATION_INFO_FAILED = -3;

    // Objects
    private StationInfoMethod task;
    private XmlPullParserHandler xmlHandler;
    private HttpURLConnection conn = null;
    private InputStream is = null;

    // Interface
    public interface StationInfoMethod {

        void setStationTaskThread(Thread thread);
        void handleStationTaskState(int state);
        void addStationInfo(Opinet.GasStnParcelable station);
        List<Opinet.GasStnParcelable> getStationList();
        int getStationIndex();
    }

    // Constructor
    StationInfoRunnable(StationInfoMethod task) {
        this.task = task;
        xmlHandler = new XmlPullParserHandler();
    }


    @Override
    public void run() {
        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        synchronized (this) {
            int index = task.getStationIndex();
            Opinet.GasStnParcelable station = task.getStationList().get(index);
            String OPINET_INFO = OPINET + "&id=" + station.getStnId();

            try {
                URL url = new URL(OPINET_INFO);
                conn = (HttpURLConnection) url.openConnection();
                //conn.setRequestProperty("Connection", "close");
                //conn.setConnectTimeout(5000);
                //conn.setReadTimeout(5000);
                //conn.connect();
                is = new BufferedInputStream(conn.getInputStream());
                //is = conn.getInputStream();
                Opinet.GasStationInfo stnInfo = xmlHandler.parseGasStationInfo(is);
                log.i("StnInfo: %s, %s", stnInfo.getOldAddrs(), stnInfo.getIsCarWash());

                station.setIsCarWash(stnInfo.getIsCarWash());
                task.addStationInfo(station);

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
                if(conn != null) conn.disconnect();

                if(index == task.getStationList().size() -1)
                    task.handleStationTaskState(DOWNLOAD_STATION_INFO_COMPLETE);
            }
        }
    }
}