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
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class StationInfoRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationInfoRunnable.class);

    // Constants
    private static final String OPINET = "http://www.opinet.co.kr/api/detailById.do?code=F186170711&out=xml";
    static final int DOWNLOAD_STATION_INFO_COMPLETE = 1;
    static final int DOWNLOAD_STATION_INFO_FAIL = -1;

    // Objects
    private Context context;
    private StationInfoMethods task;
    private XmlPullParserHandler xmlHandler;

    //Interface
    interface StationInfoMethods {
        void setStationTaskThread(Thread thread);
        void handleStationTaskState(int state);
        void setStationInfo(Opinet.GasStationInfo info);
        String getStationName();
        String getStationId();
    }

    // Constructor
    StationInfoRunnable(Context context, StationInfoMethods task) {
        this.context = context;
        this.task = task;
        xmlHandler = new XmlPullParserHandler();
    }

    @Override
    public void run() {
        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String OPINET_DETAIL = OPINET + "&id=" + task.getStationId();
        HttpURLConnection conn = null;
        InputStream is = null;

        try {
            URL url = new URL(OPINET_DETAIL);
            conn = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(conn.getInputStream());
            Opinet.GasStationInfo info = xmlHandler.parseGasStationInfo(is);
            info.setStationName(task.getStationName());
            task.setStationInfo(info);
            task.handleStationTaskState(DOWNLOAD_STATION_INFO_COMPLETE);

        } catch (MalformedURLException e) {
            log.e("MalformedURLException: %s", e.getMessage());
            task.handleStationTaskState(DOWNLOAD_STATION_INFO_FAIL);
        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
            task.handleStationTaskState(DOWNLOAD_STATION_INFO_FAIL);
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
}
