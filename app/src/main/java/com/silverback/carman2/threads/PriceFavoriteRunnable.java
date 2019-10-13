package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.XmlPullParserHandler;
import com.silverback.carman2.utils.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PriceFavoriteRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(PriceFavoriteRunnable.class);

    private static final String API_KEY = "F186170711";
    private static final String OPINET = "http://www.opinet.co.kr/api/";
    private static final String URLStn = OPINET + "detailById.do?out=xml&code="+ API_KEY + "&id=";

    // Objects
    private Context mContext;
    private StationPriceMethods mCallback;
    private XmlPullParserHandler xmlHandler;


    // Interface
    interface StationPriceMethods {
        String getStationId();
        void setStnPriceThread(Thread thread);
        void saveStationPriceData();
    }

    // Constructor
    PriceFavoriteRunnable(Context context, StationPriceMethods callback) {
        mContext = context;
        mCallback = callback;
        xmlHandler = new XmlPullParserHandler();
    }

    @Override
    public void run() {

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mCallback.setStnPriceThread(Thread.currentThread());

        final String stnId = mCallback.getStationId();
        URL url;
        InputStream in = null;
        HttpURLConnection conn = null;

        try {
            url = new URL(URLStn + stnId);
            conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();

            if(Thread.interrupted()) {
                throw new InterruptedException();
            }

            Opinet.StationPrice stnPriceData = xmlHandler.parseStationPrice(in);
            if(stnPriceData != null) {
                savePriceInfo(stnPriceData, Constants.FILE_CACHED_STATION_PRICE);
                mCallback.saveStationPriceData();
            }

        } catch(MalformedURLException e) {
            log.e("MalformedURLException: %s", e.getMessage());
        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        } catch(InterruptedException e) {
            log.e("InterruptedException: %s", e.getMessage());
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

    private void savePriceInfo(Object obj, String fName) {

        File file = new File(mContext.getApplicationContext().getCacheDir(), fName);
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
