package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.XmlPullParserHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
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
    private Context context;
    private List<Opinet.GasStnParcelable> stationList;
    private Opinet.GasStationInfo stnInfo;
    private StationInfoMethod task;
    private XmlPullParserHandler xmlHandler;
    private HttpURLConnection conn = null;
    private InputStream is = null;
    private String stationId;

    // Interface
    public interface StationInfoMethod {

        void setStationTaskThread(Thread thread);
        void handleStationTaskState(int state);
        //void setStationList(List<Opinet.GasStnParcelable> list);
        //void addCount();
        void addStationInfo(Opinet.GasStnParcelable station);
        List<Opinet.GasStnParcelable> getStationList();
        //int getStationIndex();
        //Opinet.GasStnParcelable getStation();
    }

    // Constructor
    StationInfoRunnable(Context context, StationInfoMethod task) {
        this.context = context;
        this.task = task;
        xmlHandler = new XmlPullParserHandler();
        //stationList = new ArrayList<>();
    }


    @Override
    public void run() {
        //synchronized (this) {
        task.setStationTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        //int index = task.getStationIndex();
        //station = task.getStationList().get(index);
        //String stationId = task.getStationId();
        //Opinet.GasStnParcelable station = task.getStation();

        //stationList = task.getStationList();
        //Opinet.GasStationInfo item = stationList.get(index);

        for (Opinet.GasStnParcelable station : task.getStationList()) {
            String OPINET_INFO = OPINET + "&id=" + station.getStnId();
            log.i("OPINET_INFO: %s", OPINET_INFO);

            try {
                if (Thread.interrupted()) throw new InterruptedException();
                // Adds more items of information to alreeady downloaded stations.
                Opinet.GasStationInfo stnInfo = addStationInfo(OPINET_INFO);
                station.setIsCarWash(stnInfo.getIsCarWash());
                task.addStationInfo(station);

            } catch (IOException e) {
                log.e("IOException: %s", e);
                task.handleStationTaskState(DOWNLOAD_STATION_INFO_FAILED);

            } catch (InterruptedException e) {
                log.e("InteruptedException: %s", e);
                task.handleStationTaskState(DOWNLOAD_STATION_INFO_FAILED);
            } finally {

                //task.addCount(); //increases the count for checking when to finish.
                /*
                if (task.getStationIndex() == (task.getStationList().size())) {
                    log.i("Adds Info to StationList complete: %s", index);
                    // Notifies StationTask of having the task done.
                    boolean isSaved = saveNearStationInfo(task.getStationList());

                    if (isSaved) {
                        //task.setStationList(task.getStationList());
                        task.handleStationTaskState(DOWNLOAD_STATION_INFO_COMPLETE);
                    } else {
                        task.handleStationTaskState(DOWNLOAD_STATION_INFO_FAILED);
                    }
                }
                */
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    log.e("IOException: %s", e);
                }

                if (conn != null) conn.disconnect();
            }
        }

        boolean isSaved = saveNearStationInfo(task.getStationList());

        if (isSaved) task.handleStationTaskState(DOWNLOAD_STATION_INFO_COMPLETE);
        else task.handleStationTaskState(DOWNLOAD_STATION_INFO_FAILED);

    }

    private synchronized Opinet.GasStationInfo addStationInfo(final String opinet) throws IOException {

        URL url = new URL(opinet);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Connection", "close");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();
        is = new BufferedInputStream(conn.getInputStream());

        return xmlHandler.parseGasStationInfo(is);
    }

    // Save the downloaded near station list in the designated file location.

    private boolean saveNearStationInfo(List<Opinet.GasStnParcelable> list) {

        File file = new File(context.getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);

        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);

            return true;

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        }

        //return Uri.fromFile(file);
        return false;
    }

}