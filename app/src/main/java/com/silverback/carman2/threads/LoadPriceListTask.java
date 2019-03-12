package com.silverback.carman2.threads;

import android.content.Context;
import android.net.Uri;
import android.widget.TextView;

import com.silverback.carman2.views.AvgPriceView;
import com.silverback.carman2.views.SidoPriceView;
import com.silverback.carman2.views.SigunPriceView;

import java.lang.ref.WeakReference;

public class LoadPriceListTask extends ThreadTask implements LoadPriceListRunnable.LoadPriceListMethods {

    // Constants
    //private static final String TAG = "LoadPriceListTask";

    // Objects
    private ThreadManager sThreadManager;
    private Runnable mLoadPriceListRunnable;

    private WeakReference<AvgPriceView> mWeakAvgView;
    private WeakReference<SidoPriceView> mWeakSidoView;
    private WeakReference<SigunPriceView> mWeakSigunView;
    private Uri mAvgUri, mSidoUri, mSigunUri;

    private int distSort;
    private String fuelCode, distName, price, diff;

    // Constructor: creates a LoadPriceListTask object containing LoadPriceListMethods object.
    LoadPriceListTask(Context context) {
        super();
        mLoadPriceListRunnable = new LoadPriceListRunnable(context, this);
    }

    // Initialize args to this task.
    void initLoadPriceTask(ThreadManager threadManager, TextView view, String fuelCode, Uri uri){

        // Sets this object's ThreadPool field to be the input argument
        sThreadManager = threadManager;

        // Instantiates the weak references of AvgPriceView, SidoPriceView, SigunPriceView
        /*
        if(view instanceof AvgPriceView) {
            mWeakAvgView = new WeakReference<>((AvgPriceView) view);
            mAvgUri = uri;
            distSort = 0;

        } else if(view instanceof SidoPriceView) {
            mWeakSidoView = new WeakReference<>((SidoPriceView) view);
            mSidoUri = uri;
            distSort = 1;
        } else if(view instanceof SigunPriceView) {
            mWeakSigunView = new WeakReference<>((SigunPriceView) view);
            mSigunUri = uri;
            distSort = 2;
        }
        */

        this.fuelCode = fuelCode;
    }

    // Callback method invoked by LoadPriceListMethods
    @Override
    public void setLoadPriceThread(Thread currentThread) {
        setCurrentThread(currentThread); // Inherited from ThreadTask;
    }
    @Override
    public Uri getAvgPriceUri() { return mAvgUri; }
    @Override
    public Uri getSidoPriceUri(){ return mSidoUri; }
    @Override
    public Uri getSigunPriceUri(){ return mSigunUri; }
    @Override
    public int getDistrictSort() { return distSort; }
    @Override
    public String getDefFuelCode() { return fuelCode; }

    // Method to get the Runnable object
    Runnable getLoadPriceListRunnable(){
        return mLoadPriceListRunnable;
    }

    // Getter for WeakReference to the custom views.
    AvgPriceView getAvgPriceView() { if(mWeakAvgView != null) return mWeakAvgView.get(); return null;}
    SidoPriceView getSidoPriceView() { if(mWeakSidoView != null) return mWeakSidoView.get(); return null;}
    SigunPriceView getSigunPriceView() { if(mWeakSigunView != null) return mWeakSigunView.get(); return null;}

    /*
    void recycle() {
        if(mWeakAvgView != null){
            mWeakAvgView.clear();
            mWeakAvgView = null;
        }
        if(mWeakSidoView != null) {
            mWeakSidoView.clear();
            mWeakSidoView = null;
        }
        if(mWeakSigunView != null) {
            mWeakSigunView.clear();
            mWeakSigunView = null;
        }
    }
    */

    // Setter used in LoadSavedPriceRunnable to set the price info saved in the designated URI.
    @Override
    public void setPriceInfo(String distName, String price, String diff) {
        //Log.d(TAG, "Show Price: " + price + " , " + diff);
        this.distName = distName;
        //Log.d(TAG, "District Name: " + distName);
        this.price = price;
        this.diff = diff;
    }

    @Override
    public void handleLoadPriceState(int state) {

        int outState = -1;

        switch(state) {
            case LoadPriceListRunnable.AVG_PRICE_COMPLETE:
                outState = ThreadManager.LOAD_PRICE_AVG_COMPLETED;
                break;
            case LoadPriceListRunnable.SIDO_PRICE_COMPLETE:
                outState = ThreadManager.LOAD_PRICE_SIDO_COMPLETED;
                break;
            case LoadPriceListRunnable.SIGUN_PRICE_COMPLETE:
                outState = ThreadManager.LOAD_PRICE_SIGUN_COMPLETED;
                break;
        }

        handleState(outState);
    }

    // Delegates handling the current state of the task to the ThreadManager object
    private void handleState(int state) {
        sThreadManager.handleState(this, state);
    }

    // Refers the loaded price info to ThreadManager, then passes it to each custom view.
    String[] getPriceInfo() {
        return new String[]{ distName, price, diff };
    }
}
