package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
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

public class OpinetPriceRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(OpinetPriceRunnable.class);

    //Constants
    //private static final String TAG = "OpinetPriceRunnable";

    private static final String API_KEY = "F186170711";
    private static final String OPINET = "http://www.opinet.co.kr/api/";
    private static final String URLavg = OPINET + "avgAllPrice.do?out=xml&code=" + API_KEY;
    private static final String URLsido = OPINET + "avgSidoPrice.do?out=xml&code=" + API_KEY + "&sido=";
    private static final String URLsigun = OPINET + "avgSigunPrice.do?out=xml&code=" + API_KEY + "&sido=";
    private static final String SigunCode = "&sigun=";

    private static final int AVG = 0;
    private static final int SIDO = 1;
    private static final int SIGUN = 2;

    static final int DOWNLOAD_PRICE_COMPLETE = 1;
    static final int DOWNLOAD_PRICE_FAIL = -1;

    // Objects
    private Context context;
    private XmlPullParserHandler xmlHandler;
    private OpinetPriceListMethods opinetTask;

    /*
     * An interface that defines methods that ThreadTask implements. An instance of
     * ThreadTask passes itself to an OpinetPriceRunnable instance through the
     * OpinetPriceRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    public interface OpinetPriceListMethods {
        void setPriceDownloadThread(Thread currentThread);
        String getDistrictCode();
        int getDistrictSort();
        void handlePriceTaskState(int state);
    }

    // Constructor
    OpinetPriceRunnable(Context context, OpinetPriceListMethods task) {
        this.context = context;
        opinetTask = task;
        xmlHandler = new XmlPullParserHandler();
        //mBroadcaster = new BroadcastNotifier(context.getApplicationContext());
    }

    @Override
    public void run() {

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        log.i("Thread: %s", Thread.currentThread());

        String sigunCode = opinetTask.getDistrictCode();
        String sidoCode = sigunCode.substring(0, 2);
        int district = opinetTask.getDistrictSort();
        log.i("District Code: %s, %s", sigunCode, sidoCode);

        URL url;
        InputStream in = null;
        HttpURLConnection conn = null;

        try {

            if(Thread.interrupted()) throw new InterruptedException();

            switch(district) {

                case AVG: // Average oil price
                    opinetTask.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLavg);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    List<Opinet.OilPrice> avgList = xmlHandler.parseOilPrice(in);
                    if(!avgList.isEmpty()) {
                        avgList.remove(avgList.get(3)); // Exclude Kerotene
                        savePriceInfo(avgList, Constants.FILE_CACHED_AVG_PRICE);
                        log.i("avgList: %d", avgList.size());

                    }

                    break;

                case SIDO: // Sido price
                    opinetTask.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLsido + sidoCode);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    List<Opinet.SidoPrice> sidoList = xmlHandler.parseSidoPrice(in);
                    if(!sidoList.isEmpty()) {
                        savePriceInfo(sidoList, Constants.FILE_CACHED_SIDO_PRICE);
                        log.i("sidoList: %d", sidoList.size());

                    }

                    break;

                case SIGUN: // Sigun price
                    opinetTask.setPriceDownloadThread(Thread.currentThread());
                    url = new URL(URLsigun + sidoCode + SigunCode + sigunCode);
                    conn = (HttpURLConnection)url.openConnection();
                    in = conn.getInputStream();

                    if(Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    List<Opinet.SigunPrice> sigunList = xmlHandler.parseSigunPrice(in);
                    if(!sigunList.isEmpty()) {
                        savePriceInfo(sigunList, Constants.FILE_CACHED_SIGUN_PRICE);
                        log.i("sigunList.get(0): %s", sigunList.get(0).getPrice());

                    }
                    opinetTask.handlePriceTaskState(DOWNLOAD_PRICE_COMPLETE);
                    break;
            }

        } catch (MalformedURLException e) {
            //Log.e(TAG, "Exception: " + e.getMessage());
            opinetTask.handlePriceTaskState(DOWNLOAD_PRICE_FAIL);

        } catch (IOException e) {
            //Log.e(TAG, "Exception: " + e.getMessage());
            opinetTask.handlePriceTaskState(DOWNLOAD_PRICE_FAIL);

        } catch (InterruptedException e) {
            e.printStackTrace();
            opinetTask.handlePriceTaskState(DOWNLOAD_PRICE_FAIL);

        } finally {

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

    private synchronized void savePriceInfo(List<?> list, String fName) {

        File file = new File(context.getApplicationContext().getCacheDir(), fName);
        FileOutputStream fos;
        ObjectOutputStream oos;

        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(list);
            fos.close();
            oos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
