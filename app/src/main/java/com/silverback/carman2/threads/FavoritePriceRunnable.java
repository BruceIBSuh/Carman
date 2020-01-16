package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FavoritePriceRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(FavoritePriceRunnable.class);

    private static final String API_KEY = "F186170711";
    private static final String OPINET = "http://www.opinet.co.kr/api/";
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
        URL url;
        InputStream in = null;
        HttpURLConnection conn = null;
        final File file = new File(mContext.getCacheDir(), Constants.FILE_CACHED_STATION_PRICE);

        try {
            url = new URL(URLStn + stationId);
            conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();

            if(Thread.interrupted()) {
                throw new InterruptedException();
            }

            Opinet.StationPrice stnPriceData = xmlHandler.parseStationPrice(in);
            if(stnPriceData != null) {
                log.i("new favorite: %s, %s", stnPriceData.getStnName(), stnPriceData.getDiff());
                // if the favorite placeholder becomes first, the provider saves its price in the cache.
                if(mCallback.getIsFirst()) {
                    savePriceInfo(file, stnPriceData);
                    //if(!file.exists()) savePriceInfo(stnPriceData);
                    //else saveDifferedPrice(file, stnPriceData);

                // a provider selected in FavoriteListFragment, the price of which isn't saved.
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


    // Retrieve the station price previously saved in the internal storage, then compare it
    // with the current price to calculate the price difference, which is passed to Opinet.StationPrice
    // and save the object in the internal storage
    private synchronized void saveDifferedPrice(final File stnFile, Opinet.StationPrice stationPrice) {
        Uri stnUri = Uri.fromFile(stnFile);
        try(InputStream is = mContext.getContentResolver().openInputStream(stnUri);
            ObjectInputStream ois = new ObjectInputStream(is)) {

            Opinet.StationPrice savedPrice = (Opinet.StationPrice)ois.readObject();
            log.i("Compare Station ids: %s, %s", savedPrice.getStnId(), stationId);

            if(savedPrice.getStnId().matches(stationId)) {
                Map<String, Float> current = stationPrice.getStnPrice();
                Map<String, Float> prev = savedPrice.getStnPrice();
                Map<String, Float> differedPrice = new HashMap<>();

                for (String key : current.keySet()) {
                    Float currentValue = current.get(key);
                    Float prevValue = prev.get(key);

                    log.i("price compared: %s, %s", currentValue, prevValue);

                    // Handle the null condition of both prices.
                    if(currentValue == null) throw new NullPointerException();
                    if(prevValue == null) throw new NullPointerException();

                    // Get the price difference of both prices.
                    differedPrice.put(key, currentValue - prevValue);
                }

                // Set the price differrence as params to Opinet.StationPrice setPriceDiff() and save
                // in the cached directory.
                stationPrice.setPriceDiff(differedPrice);
            }

            savePriceInfo(stnFile, stationPrice);


        } catch(FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e);
        } catch(IOException e) {
            log.e("IOException: %s", e);
        } catch(ClassNotFoundException e) {
            log.e("ClassNotFoundException: %s", e);
        } catch(NullPointerException e) {
            log.e("NullPointerException: %s", e);
        }

    }



}
