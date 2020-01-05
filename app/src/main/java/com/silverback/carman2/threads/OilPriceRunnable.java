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

public class OilPriceRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(OilPriceRunnable.class);

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
     * ThreadTask passes itself to an OilPriceRunnable instance through the
     * OilPriceRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    public interface OpinetPriceListMethods {
        void setPriceDownloadThread(Thread currentThread);
        void handlePriceTaskState(int state);
        void setOilPrice(int sort, Object obj);

        int getTaskCount();
        String getDistrictCode();
        String getStationId();
    }

    // Constructor
    OilPriceRunnable(Context context, OpinetPriceListMethods task, int category) {
        this.context = context.getApplicationContext();
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
            switch(category) {

                case AVG: // Average oil price
                    log.i("AvgPrice thread: %s", Thread.currentThread());
                    task.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLavg);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) throw new InterruptedException();

                    List<Opinet.OilPrice> avgList = xmlHandler.parseOilPrice(in);
                    if(!avgList.isEmpty()) {
                        avgList.remove(avgList.get(3)); // Exclude Kerotene
                        //task.handlePriceTaskState(DOWNLOAD_AVG_PRICE_COMPLETE);
                        //task.setAvgPrice(avgList);
                        savePriceInfo(avgList, Constants.FILE_CACHED_AVG_PRICE);
                    }

                    task.setOilPrice(0, avgList);
                    break;

                case SIDO: // Sido price
                    log.i("SidoPrice thread: %s", Thread.currentThread());
                    task.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLsido + sidoCode);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) throw new InterruptedException();

                    List<Opinet.SidoPrice> sidoList = xmlHandler.parseSidoPrice(in);
                    if(!sidoList.isEmpty()) {
                        //task.handlePriceTaskState(DOWNLOAD_SIDO_PRICE_COMPLETE);
                        //task.setSidoPrice(sidoList);
                        savePriceInfo(sidoList, Constants.FILE_CACHED_SIDO_PRICE);
                    }

                    task.setOilPrice(1, sidoList);
                    break;

                case SIGUN: // Sigun price
                    log.i("sigunPrice thread: %s", Thread.currentThread());
                    task.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLsigun + sidoCode + SigunCode + sigunCode);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) throw new InterruptedException();

                    List<Opinet.SigunPrice> sigunList = xmlHandler.parseSigunPrice(in);
                    if(!sigunList.isEmpty()) {
                        //task.handlePriceTaskState(DOWNLOAD_SIGUN_PRICE_COMPLETE);
                        //task.setSigunPrice(sigunList);
                        savePriceInfo(sigunList, Constants.FILE_CACHED_SIGUN_PRICE);
                    }

                    task.setOilPrice(2, sigunList);
                    break;

                case STATION:
                    log.i("Station price thread:%s", Thread.currentThread());
                    task.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLStn + stnId);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) throw new InterruptedException();

                    Opinet.StationPrice stnPrice = xmlHandler.parseStationPrice(in);
                    if(stnPrice != null) {
                        log.i("Station Price: %s, %s", stnPrice.getStnName(), stnPrice.getStnPrice());
                        savePriceInfo(stnPrice, Constants.FILE_CACHED_STATION_PRICE);
                    }

                    task.setOilPrice(3, stnPrice);
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

        File file = new File(context.getCacheDir(), fName);

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)){
            oos.writeObject(obj);
        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());
        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
        }
    }

}
