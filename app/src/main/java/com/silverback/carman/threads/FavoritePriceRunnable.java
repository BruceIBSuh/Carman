package com.silverback.carman.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.XmlPullParserHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class FavoritePriceRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavoritePriceRunnable.class);

    private static final String API_KEY = "F186170711";
    private static final String OPINET = "https://www.opinet.co.kr/api/";
    private static final String URLStn = OPINET + "detailById.do?out=xml&code="+ API_KEY + "&id=";

    // Objects
    private final Context mContext;
    private final StationPriceMethods mCallback;
    private final XmlPullParserHandler xmlHandler;

    // Interface
    interface StationPriceMethods {
        String getStationId();
        boolean getIsFirst();
        void setStnPriceThread(Thread thread);
        void setFavoritePrice(Map<String, Float> data);
        void savePriceDiff();
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
        String stationId = mCallback.getStationId();

        InputStream in = null;
        HttpURLConnection conn = null;
        final File file = new File(mContext.getCacheDir(), Constants.FILE_CACHED_FAV_PRICE);
        //final File file = new File(mContext.getFilesDir(), Constants.FILE_FAVORITE_PRICE);
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
        } catch(IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(in != null) {
                try { in.close();}
                catch (IOException e) { e.printStackTrace(); }
            }

            if(conn != null) conn.disconnect();
        }
    }

    private void savePriceInfo(File file, Object obj) {
        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
            mCallback.savePriceDiff();
        } catch (IOException e) { e.printStackTrace(); }
    }
}
