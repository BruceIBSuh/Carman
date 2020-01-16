package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GasPriceRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasPriceRunnable.class);

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
     * ThreadTask passes itself to an GasPriceRunnable instance through the
     * GasPriceRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    public interface OpinetPriceListMethods {
        void setPriceDownloadThread(Thread currentThread);
        void handlePriceTaskState(int state);
        void addPriceCount();
        int getTaskCount();
        String getDistrictCode();
        String getStationId();
    }

    // Constructor
    GasPriceRunnable(Context context, OpinetPriceListMethods task, int category) {
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
                        log.i("average price fetched");
                        avgList.remove(avgList.get(3)); // Exclude Kerotene
                        savePriceInfo(avgList, Constants.FILE_CACHED_AVG_PRICE);
                    } else {
                        log.i("failed to fetch the average price");
                    }

                    task.addPriceCount();
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
                        log.i("Sido price fetched");
                        savePriceInfo(sidoList, Constants.FILE_CACHED_SIDO_PRICE);
                    }

                    task.addPriceCount();
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
                        log.i("sigun price list fetched");
                        savePriceInfo(sigunList, Constants.FILE_CACHED_SIGUN_PRICE);
                    }

                    task.addPriceCount();
                    break;

                case STATION:
                    if(stnId != null) {
                        log.i("Station price thread:%s", Thread.currentThread());
                        task.setPriceDownloadThread(Thread.currentThread());
                        url = new URL(URLStn + stnId);
                        conn = (HttpURLConnection) url.openConnection();
                        in = conn.getInputStream();

                        if (Thread.interrupted()) throw new InterruptedException();

                        Opinet.StationPrice stationPrice = xmlHandler.parseStationPrice(in);
                        if (stationPrice != null) {
                            // Save the object in the cache with the price difference if the favorite
                            // gas stqation is left unchanged.
                            saveStationPriceDiff(stationPrice);
                        }
                    }

                    task.addPriceCount();
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

            if(task.getTaskCount() == 4) {
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

    // Save price data
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


    // As with the first-set favorite station, compare the current station id with the id previous
    // id from saved in the internal cache storage to check whether it has unchanged.
    // If it is unchanged, calculate the price difference b/w the current and the previously saved
    // station and pass it to setPriceDiff() in Opinet.StationPrice, then save the object.
    // Otherwise, just save the object the same as the other prices.
    private void saveStationPriceDiff(Opinet.StationPrice stnPrice) {

        File stnFile = new File(context.getCacheDir(), Constants.FILE_CACHED_STATION_PRICE);
        Uri stnUri = Uri.fromFile(stnFile);

        try(InputStream is = context.getContentResolver().openInputStream(stnUri);
            ObjectInputStream ois = new ObjectInputStream(is)) {

            Opinet.StationPrice savedPrice = (Opinet.StationPrice)ois.readObject();
            log.i("prevPrice: %s", savedPrice);
            // Check if the station is unchanged.
            if(stnPrice.getStnId().matches(savedPrice.getStnId())) {
                log.i("Same favorite station");
                Map<String, Float> current = stnPrice.getStnPrice();
                Map<String, Float> prev = savedPrice.getStnPrice();
                Map<String, Float> diffPrice = new HashMap<>();
                // Calculate the price differences based on the fuel codes.
                for (String key : current.keySet()) {
                    Float currentValue = current.get(key);
                    Float prevValue = prev.get(key);

                    // Throw the exception if the price is null.
                    if(currentValue == null) throw new NullPointerException();
                    if(prevValue == null) throw new NullPointerException();

                    // Get the price difference of both prices.
                    diffPrice.put(key, currentValue - prevValue);
                    stnPrice.setPriceDiff(diffPrice);
                }

            } else {
                log.i("Favorite station changes");
            }

            savePriceInfo(stnPrice, Constants.FILE_CACHED_STATION_PRICE);

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
