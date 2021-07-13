package com.silverback.carman.threads;

import android.content.Context;
import android.os.Process;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.XmlPullParserHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This Runnable class is to retrieve the price data from the Opinet server and save the district
 * price data in the cache directory and the station data in the internal file directory.
 */
public class GasPriceRunnable implements Runnable {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GasPriceRunnable.class);

    // constants
    private static final String API_KEY = "F186170711";
    private static final String OPINET = "https://www.opinet.co.kr/api/";
    private static final String URLavg = OPINET + "avgAllPrice.do?out=xml&code=" + API_KEY;
    private static final String URLsido = OPINET + "avgSidoPrice.do?out=xml&code=" + API_KEY + "&sido=";
    private static final String URLsigun = OPINET + "avgSigunPrice.do?out=xml&code=" + API_KEY + "&sido=";
    private static final String SigunCode = "&sigun=";
    private static final String URLstn = OPINET + "detailById.do?out=xml&code="+ API_KEY + "&id=";

    static final int AVG = 0;
    static final int SIDO = 1;
    static final int SIGUN = 2;
    static final int STATION = 3;

    static final int DOWNLOAD_PRICE_COMPLETE = 1;
    static final int DOWNLOAD_PRICE_FAILED = -1;

    // Objects
    private final Context context;
    private final XmlPullParserHandler xmlHandler;
    private final OpinetPriceListMethods task;

    // Fields
    private final int category;

    /*
     * An interface that defines methods that ThreadTask implements. An instance of
     * ThreadTask passes itself to an GasPriceRunnable instance through the
     * GasPriceRunnable constructor, after which the two instances can access each other's
     * variables.
     */
    public interface OpinetPriceListMethods {
        void setGasPriceThread(Thread currentThread);
        void handlePriceTaskState(int state);
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
        log.i("District code: %s, %s, %s", sigunCode, sidoCode, stnId);

        try {
            switch(category) {
                case AVG:
                    task.setGasPriceThread(Thread.currentThread());
                    if(Thread.interrupted()) throw new InterruptedException();

                    URL avgURL = new URL(URLavg);
                    HttpURLConnection avgConn = (HttpURLConnection)avgURL.openConnection();
                    try(InputStream avgIn = avgConn.getInputStream()) {
                        List<Opinet.OilPrice> avgList = xmlHandler.parseOilPrice(avgIn);
                        if (!avgList.isEmpty()) {
                            avgList.remove(avgList.get(3)); // Exclude Kerotene
                            savePriceInfo(avgList, Constants.FILE_CACHED_AVG_PRICE);
                            task.handlePriceTaskState(DOWNLOAD_PRICE_COMPLETE);
                        } else task.handlePriceTaskState(DOWNLOAD_PRICE_FAILED);
                    } catch(IOException e) {e.printStackTrace();}

                    avgConn.disconnect();
                    break;

                case SIDO:
                    task.setGasPriceThread(Thread.currentThread());
                    if(Thread.interrupted()) throw new InterruptedException();

                    URL sidoURL = new URL(URLsido + sidoCode);
                    HttpURLConnection sidoConn = (HttpURLConnection)sidoURL.openConnection();
                    try(InputStream sidoIn = sidoConn.getInputStream()) {
                        List<Opinet.SidoPrice> sidoList = xmlHandler.parseSidoPrice(sidoIn);
                        if (sidoList.size() > 0) {
                            savePriceInfo(sidoList, Constants.FILE_CACHED_SIDO_PRICE);
                            task.handlePriceTaskState(DOWNLOAD_PRICE_COMPLETE);
                        } else task.handlePriceTaskState(DOWNLOAD_PRICE_FAILED);
                    } catch(IOException e) {e.printStackTrace();}

                    sidoConn.disconnect();
                    break;

                case SIGUN:
                    task.setGasPriceThread(Thread.currentThread());
                    if(Thread.interrupted()) throw new InterruptedException();

                    URL sigunURL = new URL(URLsigun + sidoCode + SigunCode + sigunCode);
                    HttpURLConnection sigunConn = (HttpURLConnection)sigunURL.openConnection();
                    try(InputStream sigunIn = sigunConn.getInputStream()) {
                        List<Opinet.SigunPrice> sigunList = xmlHandler.parseSigunPrice(sigunIn);
                        if (sigunList.size() > 0) {
                            savePriceInfo(sigunList, Constants.FILE_CACHED_SIGUN_PRICE);
                            task.handlePriceTaskState(DOWNLOAD_PRICE_COMPLETE);
                            sigunConn.disconnect();
                        } else task.handlePriceTaskState(DOWNLOAD_PRICE_FAILED);
                    } catch(IOException e) {e.printStackTrace();}

                    sigunConn.disconnect();
                    break;

                case STATION:
                    if(stnId != null) {
                        task.setGasPriceThread(Thread.currentThread());
                        if (Thread.interrupted()) throw new InterruptedException();

                        URL stnURL = new URL(URLstn + stnId);
                        HttpURLConnection stnConn = (HttpURLConnection) stnURL.openConnection();
                        try (InputStream stnIn = stnConn.getInputStream()) {
                            Opinet.StationPrice stnPrice = xmlHandler.parseStationPrice(stnIn);
                            if (stnPrice != null) {
                                // Save the object in the cache with the price difference if the favorite
                                // gas stqation is left unchanged.
                                saveStationPriceDiff(stnPrice);
                                task.handlePriceTaskState(DOWNLOAD_PRICE_COMPLETE);
                            } else task.handlePriceTaskState(DOWNLOAD_PRICE_FAILED);
                        }catch(IOException e) {e.printStackTrace();}

                        stnConn.disconnect();

                    } else task.handlePriceTaskState(DOWNLOAD_PRICE_FAILED);
                    break;
            }

        } catch (IOException | InterruptedException e) { e.printStackTrace();}
    }

    // Save the district price data in the cache directory to avoid frequent access to the Optinet,
    // which will be deleted when leaving the app.
    private synchronized void savePriceInfo(Object obj, String fName) {
        File file = new File(context.getCacheDir(), fName);
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)){
            oos.writeObject(obj);
        } catch (IOException e) { e.printStackTrace();}
    }

    // As with the first-placeholder favorite station, compare the current station id with the id
    // from saved in the internal file storage to check whether it has unchanged.
    // If it remains unchanged, calculate the difference b/w the current and the previously saved price
    // and pass it to setPriceDiff() in Opinet.StationPrice, then save the object. Otherwise, jgasust
    // save the object the same as the other prices.
    private void saveStationPriceDiff(Opinet.StationPrice stnPrice) {
        final String fileName = Constants.FILE_FAVORITE_PRICE;
        //final File stnFile = new File(context.getFilesDir(), fileName);
        try(FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis)) {
            Opinet.StationPrice savedStn = (Opinet.StationPrice)ois.readObject();
            // The first favorite station has not changed.
            if(stnPrice.getStnId().equalsIgnoreCase(savedStn.getStnId())) {
                log.i("Same favorite station");
                Map<String, Float> current = stnPrice.getStnPrice();
                Map<String, Float> prev = savedStn.getStnPrice();
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
            }

        // If no file initially exists, throws IOException, then create new filew.
        } catch(IOException | ClassNotFoundException | NullPointerException e) {e.printStackTrace();}

        // Once getting the price difference b/w the current and previous price, save Opinet.StationPrice
        // in the internal storage to display the price view in the viewpager of GeneralFragment.
        try(FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(stnPrice);
        } catch(IOException e){e.printStackTrace();}

    }


}
