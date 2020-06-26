package com.silverback.carman2.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.Opinet;
import com.silverback.carman2.viewmodels.XmlPullParserHandler;
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
import java.util.Map;

public class FavoritePriceRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavoritePriceRunnable.class);

    private static final String API_KEY = "F186170711";
    private static final String OPINET = "https://www.opinet.co.kr/api/";
    private static final String URLStn = OPINET + "detailById.do?out=xml&code="+ API_KEY + "&id=";

    // Objects
    private Context mContext;
    private StationPriceMethods mCallback;
    private XmlPullParserHandler xmlHandler;

    // Fields
    private String stationId;

    // Interface
    interface StationPriceMethods {
        String getStationId();
        boolean getIsFirst();
        void setStnPriceThread(Thread thread);
        void setFavoritePrice(Map<String, Float> data);
        void saveDifferedPrice();
    }

    // Constructor
    FavoritePriceRunnable(Context context, StationPriceMethods callback) {
        mContext = context;
        mCallback = callback;
        xmlHandler = new XmlPullParserHandler();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mCallback.setStnPriceThread(Thread.currentThread());
        stationId = mCallback.getStationId();

        InputStream in = null;
        HttpURLConnection conn = null;
        //final File file = new File(mContext.getCacheDir(), Constants.FILE_CACHED_STATION_PRICE);
        final File file = new File(mContext.getFilesDir(), Constants.FILE_FAVORITE_PRICE);

        try {
            if(Thread.interrupted()) throw new InterruptedException();

            URL url = new URL(URLStn + stationId);
            conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();

            Opinet.StationPrice stnPriceData = xmlHandler.parseStationPrice(in);
            if(stnPriceData != null) {
                log.i("new favorite: %s, %s", stnPriceData.getStnName(), stnPriceData.getPriceDiff());
                // if the favorite placeholder becomes first, the provider saves its price in the cache.
                if(mCallback.getIsFirst()) {
                    savePriceInfo(file, stnPriceData);
                    //if(!file.exists()) savePriceInfo(stnPriceData);
                    //else saveDifferedPrice(file, stnPriceData);

                // A favorite station selected in FavoriteListFragment, the price of which isn't saved.
                } else mCallback.setFavoritePrice(stnPriceData.getStnPrice());

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

    private void savePriceInfo(final File file, Object obj) {
        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
            mCallback.saveDifferedPrice();

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());
        } catch (IOException e) {
            log.e("SavePriceInfo IOException: %s", e.getMessage());
        }
    }
}
