package com.silverback.carman.threads;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.XmlPullParserHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

        try {
            if(Thread.interrupted()) throw new InterruptedException();
            URL url = new URL(URLStn + stationId);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            try(InputStream in = conn.getInputStream()) {
                Opinet.StationPrice currentData = xmlHandler.parseStationPrice(in);
                //if(stnPriceData != null) {
                final String stnName = currentData.getStnName();
                final Map<String, Float> stnPrice = currentData.getStnPrice();
                // The first favorite station which is to display in the MainActivity viewpager
                // with the price info.
                if(mCallback.getIsFirst()) {
                    final File file = new File(mContext.getCacheDir(), Constants.FILE_CACHED_FAV_PRICE);
                    if(!file.exists()) savePriceInfo(file, currentData);
                    else {
                        Uri uri = Uri.fromFile(file);
                        try(InputStream is = mContext.getContentResolver().openInputStream(uri);
                            ObjectInputStream ois = new ObjectInputStream(is)) {
                            Opinet.StationPrice prevData = (Opinet.StationPrice)ois.readObject();
                            if(Objects.equals(prevData.getStnName(), stnName)){
                                log.i("get the price difference");
                                Map<String, Float> current = currentData.getStnPrice();
                                Map<String, Float> prev = prevData.getStnPrice();
                                Map<String, Float> diffPrice = new HashMap<>();

                                for (String key : current.keySet()) {
                                    Float currentValue = current.get(key);
                                    Float prevValue = prev.get(key);
                                    if (currentValue == null) throw new NullPointerException();
                                    if (prevValue == null) throw new NullPointerException();
                                    diffPrice.put(key, currentValue - prevValue);
                                    log.i("price diff: %s",  currentValue - prevValue);
                                    currentData.setPriceDiff(diffPrice);
                                }
                            }

                            savePriceInfo(file, currentData);

                        } catch(IOException | ClassNotFoundException | NullPointerException e) {
                            e.printStackTrace();
                        }

                    }
                } else mCallback.setFavoritePrice(currentData.getStnPrice());
            } finally { if(conn != null) conn.disconnect(); }

        } catch(IOException | InterruptedException e){e.printStackTrace(); }
    }

    private void savePriceInfo(File file, Object obj) {
        log.i("save price info");
        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
            mCallback.savePriceDiff();
        } catch (IOException e) { e.printStackTrace(); }
    }

}
