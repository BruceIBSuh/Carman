package com.silverback.carman2.threads;

import android.os.Process;

import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.models.Opinet;

import java.util.List;

public class StationListRunnable implements Runnable {

    // Constants
    //private static final String TAG = "NearStationList";
    static final int POPULATE_STATION_LIST_COMPLETE = 1;
    static final int POPULATE_STATION_LIST_FAIL = -1;

    // Objects
    private StationListMethod mTask;

    // Interface
    public interface StationListMethod {

        List<Opinet.GasStnParcelable> getStationList();
        StationListAdapter getStationListAdapter();
        void setStationListThread(Thread thread);
        void handleListTaskState(int state);

    }

    // Constructor
    StationListRunnable(StationListMethod task) {
        //Log.i(TAG, "Station list:");
        mTask = task;
    }


    @Override
    public void run() {

        mTask.setStationListThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        //Log.d(TAG, "Thread: " + Thread.currentThread());

        try {

            if(Thread.interrupted()) throw new InterruptedException();

            StationListAdapter mAdapter = mTask.getStationListAdapter();
            List<Opinet.GasStnParcelable> mStationList = mTask.getStationList();

            for (int pos = 0; pos < mStationList.size(); pos++) {
                String stnId = mStationList.get(pos).getStnId();
                String stnCode = mStationList.get(pos).getStnCode();
                String stnName = mStationList.get(pos).getStnName();
                float oilPrice = mStationList.get(pos).getStnPrice();
                float distance = mStationList.get(pos).getDist();
                float longitude = mStationList.get(pos).getLongitude();
                float latitude = mStationList.get(pos).getLatitude();
                //int imgResource = getGasStationImage(stnCode);

                //Log.i(TAG, "Station Info: " + stnId + "," + stnCode + "," + stnName);

                //mAdapter.addItem(pos, stnId, imgResource, stnName, oilPrice, distance, longitude, latitude);
            }

            //mAdapter.notifyDataSetChanged(); //Only the original thread that created a view hierarchy can touch its views.
            mTask.handleListTaskState(POPULATE_STATION_LIST_COMPLETE);

        } catch (InterruptedException e) {
            //Log.e(TAG, "InteruptedException: " + e.getMessage());
            mTask.handleListTaskState(POPULATE_STATION_LIST_FAIL);

        } finally {
            mTask.setStationListThread(null);
            Thread.interrupted();
        }

    }

    // Method for setting logo image to each oil company using enum..
    /*
    private static int getGasStationImage(String name) {

        int resId = -1;
        switch(name) {
            case "SKE": resId = R.drawable.logo_sk; break;
            case "GSC": resId = R.drawable.logo_gs; break;
            case "HDO": resId = R.drawable.logo_hyundai; break;
            case "SOL": resId = R.drawable.logo_soil; break;
            case "RTO": resId = R.drawable.logo_pb; break;
            case "RTX": resId = R.drawable.logo_express; break;
            case "NHO": resId = R.drawable.logo_nonghyup; break;
            case "E1G": resId = R.drawable.logo_e1g; break;
            case "SKG": resId = R.drawable.logo_skg; break;
            case "ETC": resId = R.drawable.logo_anonym; break;
            default: break;
        }

        return resId;
    }
    */

}