package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;

import com.silverback.carman2.R;
import com.silverback.carman2.models.Opinet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class LoadPriceListRunnable implements Runnable {

    // Constants
    //private static final String TAG = "LoadPriceListRunnable";

    static final int AVG_PRICE_COMPLETE = 1;
    static final int SIDO_PRICE_COMPLETE = 2;
    static final int SIGUN_PRICE_COMPLETE = 3;

    // Objects
    private Context context;
    private LoadPriceListMethods mLoadTask;

    // Interface
    public interface LoadPriceListMethods {

        void setLoadPriceThread(Thread currentThread);
        String getDefFuelCode();
        int getDistrictSort();

        Uri getAvgPriceUri();
        Uri getSidoPriceUri();
        Uri getSigunPriceUri();

        void setPriceInfo(String distName, String price, String fuelCode);
        void handleLoadPriceState(int state);
    }

    /*
     * This constructor creates an instance of LoadPriceListRunnable and stores in it a reference
     * to the PhotoTask instance that instantiated it.
     * @param context, which is used to instantiate InputStream
     * @param photoTask The PhotoTask, which implements TaskRunnableDecodeMethods
     */
    LoadPriceListRunnable (Context context, LoadPriceListMethods task) {
        this.context = context;
        mLoadTask = task;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        //synchronized (this) { // Required to be checked, not for sure whether to use syhchroized.
        int distSort = mLoadTask.getDistrictSort();
        String fuelCode = mLoadTask.getDefFuelCode();

        InputStream is = null;
        ObjectInputStream ois = null;

        // Consider refactoring!!
        try {
            synchronized(this) {
                switch (distSort) {
                    case 0:
                        mLoadTask.setLoadPriceThread(Thread.currentThread());

                        Uri avgUri = mLoadTask.getAvgPriceUri();
                        is = context.getContentResolver().openInputStream(avgUri);
                        ois = new ObjectInputStream(is);
                        List<Opinet.OilPrice> avgPrice = (List<Opinet.OilPrice>) ois.readObject();

                        for (Opinet.OilPrice opinet : avgPrice) {
                            if (opinet.getProductCode().matches(fuelCode)) {
                                //String distName = context.getResources().getString(R.string.main_subtitle_avg_price);
                                String price = Float.toString(opinet.getPrice());
                                String diff = Float.toString(opinet.getDiff());
                                //mLoadTask.setPriceInfo(distName, price, diff);
                                break;
                            }
                        }

                        mLoadTask.handleLoadPriceState(AVG_PRICE_COMPLETE);
                        //Log.d(TAG, "avgUri complete");
                        break;

                    case 1:
                        mLoadTask.setLoadPriceThread(Thread.currentThread());

                        Uri sidoUri = mLoadTask.getSidoPriceUri();
                        is = context.getContentResolver().openInputStream(sidoUri);
                        ois = new ObjectInputStream(is);
                        List<Opinet.SidoPrice> sidoPrice = (List<Opinet.SidoPrice>) ois.readObject();

                        for (Opinet.SidoPrice opinet : sidoPrice) {
                            if (opinet.getProductCd().matches(fuelCode)) {
                                String sidoName = opinet.getSidoName();
                                String price = Float.toString(opinet.getPrice());
                                String diff = Float.toString(opinet.getDiff());
                                mLoadTask.setPriceInfo(sidoName, price, diff);
                                break;
                            }
                        }
                        mLoadTask.handleLoadPriceState(SIDO_PRICE_COMPLETE);
                        //Log.d(TAG, "sido complete");
                        break;

                    case 2:
                        mLoadTask.setLoadPriceThread(Thread.currentThread());

                        Uri sigunUri = mLoadTask.getSigunPriceUri();
                        is = context.getContentResolver().openInputStream(sigunUri);
                        ois = new ObjectInputStream(is);
                        List<Opinet.SigunPrice> sigunPrice = (List<Opinet.SigunPrice>) ois.readObject();

                        for (Opinet.SigunPrice opinet : sigunPrice) {
                            if (opinet.getProductCd().matches(fuelCode)) {
                                String sigunName = opinet.getSigunName();
                                String price = Float.toString(opinet.getPrice());
                                String diff = Float.toString(opinet.getDiff());
                                mLoadTask.setPriceInfo(sigunName, price, diff);
                                break;
                            } else {
                                mLoadTask.setPriceInfo(opinet.getSigunName(), null, null);
                            }
                        }

                        mLoadTask.handleLoadPriceState(SIGUN_PRICE_COMPLETE);
                        //Log.d(TAG, "sigun complete");
                        break;
                    default:
                        break;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
