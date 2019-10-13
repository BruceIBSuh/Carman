package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
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

public class PriceRegionalRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(PriceRegionalRunnable.class);

    // constants
    private static final String API_KEY = "F186170711";
    private static final String OPINET = "http://www.opinet.co.kr/api/";
    private static final String URLavg = OPINET + "avgAllPrice.do?out=xml&code=" + API_KEY;
    private static final String URLsido = OPINET + "avgSidoPrice.do?out=xml&code=" + API_KEY + "&sido=";
    private static final String URLsigun = OPINET + "avgSigunPrice.do?out=xml&code=" + API_KEY + "&sido=";
    private static final String SigunCode = "&sigun=";
    private static final String URLStn = OPINET + "detailById.do?out=xml&code="+ API_KEY + "&id=";


    static final int AVG = 0;
    static final int SIDO = 1;
    static final int SIGUN = 2;
    static final int STATION = 3;

    static final int DOWNLOAD_PRICE_COMPLETE = 1;
    static final int DOWNLOAD_PRICE_FAILED = -1;

    // Objects
    private Context context;
    private XmlPullParserHandler xmlHandler;
    private OpinetPriceListMethods task;

    // Fields
    private int category;

    /*
     * An interface that defines methods that ThreadTask implements. An instance of
     * ThreadTask passes itself to an PriceRegionalRunnable instance through the
     * PriceRegionalRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    public interface OpinetPriceListMethods {
        void setPriceDownloadThread(Thread currentThread);
        void handlePriceTaskState(int state);
        void setTaskCount();
        int getTaskCount();
        String getDistrictCode();
        String getStationId();
    }

    // Constructor
    PriceRegionalRunnable(Context context, OpinetPriceListMethods task, int category) {
        this.context = context;
        this.category = category;
        this.task = task;
        xmlHandler = new XmlPullParserHandler();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        String sigunCode = task.getDistrictCode();
        String sidoCode = sigunCode.substring(0, 2);
        String stnId = task.getStationId();

        URL url;
        InputStream in = null;
        HttpURLConnection conn = null;

        try {
            if(Thread.interrupted()) throw new InterruptedException();

            switch(category) {

                case AVG: // Average oil price
                    log.i("AvgPrice thread: %s", Thread.currentThread());
                    task.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLavg);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    List<Opinet.OilPrice> avgList = xmlHandler.parseOilPrice(in);
                    if(!avgList.isEmpty()) {
                        avgList.remove(avgList.get(3)); // Exclude Kerotene
                        //task.handlePriceTaskState(DOWNLOAD_AVG_PRICE_COMPLETE);
                        task.setTaskCount();
                        savePriceInfo(avgList, Constants.FILE_CACHED_AVG_PRICE);
                    }

                    break;

                case SIDO: // Sido price
                    log.i("SidoPrice thread: %s", Thread.currentThread());
                    task.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLsido + sidoCode);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();


                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    List<Opinet.SidoPrice> sidoList = xmlHandler.parseSidoPrice(in);
                    if(!sidoList.isEmpty()) {
                        //task.handlePriceTaskState(DOWNLOAD_SIDO_PRICE_COMPLETE);
                        task.setTaskCount();
                        savePriceInfo(sidoList, Constants.FILE_CACHED_SIDO_PRICE);
                    }

                    break;

                case SIGUN: // Sigun price
                    log.i("sigunPrice thread: %s", Thread.currentThread());
                    task.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLsigun + sidoCode + SigunCode + sigunCode);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    List<Opinet.SigunPrice> sigunList = xmlHandler.parseSigunPrice(in);
                    if(!sigunList.isEmpty()) {
                        //task.handlePriceTaskState(DOWNLOAD_SIGUN_PRICE_COMPLETE);
                        task.setTaskCount();
                        savePriceInfo(sigunList, Constants.FILE_CACHED_SIGUN_PRICE);
                    }

                    break;

                case STATION:
                    log.i("Station price thread:%s", Thread.currentThread());
                    task.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLStn + stnId);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    Opinet.StationPrice stnPrice = xmlHandler.parseStationPrice(in);
                    if(stnPrice != null) {
                        log.i("Station Price: %s", stnPrice.getStnName());
                        task.setTaskCount();
                        savePriceInfo(stnPrice, Constants.FILE_CACHED_STATION_PRICE);
                    }

                    break;
            }


        } catch (MalformedURLException e) {
            log.e("MalformedURLException: %s", e.getMessage());
            task.handlePriceTaskState(DOWNLOAD_PRICE_FAILED);

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
            task.handlePriceTaskState(DOWNLOAD_PRICE_FAILED);

        } catch (InterruptedException e) {
            log.e("InterruptedException: %s", e.getMessage());
            task.handlePriceTaskState(DOWNLOAD_PRICE_FAILED);

        } finally {

            if(task.getTaskCount() == 3) {
                log.i("Runnable count: %s", task.getTaskCount());
                task.handlePriceTaskState(DOWNLOAD_PRICE_COMPLETE);
            }

            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(conn != null) conn.disconnect();
        }



    }

    private synchronized void savePriceInfo(Object obj, String fName) {

        File file = new File(context.getApplicationContext().getCacheDir(), fName);
        FileOutputStream fos;
        ObjectOutputStream oos;

        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
            fos.close();
            oos.close();

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());
        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
        }
    }

}
