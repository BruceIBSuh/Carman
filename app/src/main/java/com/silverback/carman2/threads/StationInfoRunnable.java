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
import java.net.MalformedURLException;
import java.net.URL;

public class StationInfoRunnable implements Runnable {

    // Logging
    static private final LoggingHelper log = LoggingHelperFactory.create(StationInfoRunnable.class);

    // Constants
    private static final String OPINET = "http://www.opinet.co.kr/api/detailById.do?code=F186170711&out=xml";
    static final int DOWNLOAD_STN_INFO_COMPLTED = 1;
    static final int DOWNLOAD_STN_INFO_FAILED = -1;

    // Objects
    private Context context;
    private StationInfoMethod mTask;
    private XmlPullParserHandler xmlHandler;

    // Interface
    public interface StationInfoMethod {
        String getStationId();
        int getIndex();
        void setTaskThread(Thread thread);
        void setStationInfo(Opinet.GasStationInfo stnInfo);
        void handleTaskState(int state);
    }

    // Constructor
    StationInfoRunnable(StationInfoMethod task) {
        mTask = task;
        xmlHandler = new XmlPullParserHandler();
    }


    @Override
    public void run() {

        mTask.setTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String stnId = mTask.getStationId();
        final int index = mTask.getIndex();
        final String OPINET_INFO = OPINET + "&id=" + stnId;

        HttpURLConnection conn;
        InputStream is = null;

        try {

            if (Thread.interrupted()) throw new InterruptedException();

            final URL url = new URL(OPINET_INFO);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            is = conn.getInputStream();

            Opinet.GasStationInfo info = xmlHandler.parseGasStationInfo(is);
            mTask.setStationInfo(info);

            log.i("Thread and Parameters: %s, %s, %s", Thread.currentThread(), stnId, index);
            log.i("GasStationInfo: %s, %s", info.getIsCarWash(), info.getTelNo());

            mTask.handleTaskState(DOWNLOAD_STN_INFO_COMPLTED);

        } catch (MalformedURLException e) {
            log.e("MalformedURLException: %s", e);
            mTask.handleTaskState(DOWNLOAD_STN_INFO_FAILED);
        } catch (IOException e) {
            log.e("IOException: %s", e);
            mTask.handleTaskState(DOWNLOAD_STN_INFO_FAILED);

        } catch (InterruptedException e) {
            log.e("InteruptedException: %s", e);
            mTask.handleTaskState(DOWNLOAD_STN_INFO_FAILED);
        } finally {
            try {
                if(is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //if(conn != null) conn.disconnect();
        }
    }


}