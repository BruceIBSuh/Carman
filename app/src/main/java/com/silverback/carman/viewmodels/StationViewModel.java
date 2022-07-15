package com.silverback.carman.viewmodels;

import android.util.SparseArray;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationEvRunnable;
import com.silverback.carman.threads.StationFavRunnable;
import com.silverback.carman.threads.StationGasRunnable;
import com.silverback.carman.threads.StationHydroRunnable;
import com.silverback.carman.threads.StationInfoRunnable;

import java.util.List;

public class StationViewModel extends ViewModel {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationViewModel.class);

    // Objects
    //private MutableLiveData<List<Opinet.GasStnParcelable>> stnList;
    private MutableLiveData<List<StationGasRunnable.Item>> stnList;
    private MutableLiveData<SparseArray<StationInfoRunnable.Info>> stationInfoList;
    private MutableLiveData<StationFavRunnable.Info> favStationInfo;

    //private MutableLiveData<Opinet.GasStnParcelable> currentStation;
    private MutableLiveData<StationGasRunnable.Item> currentStation;
    private MutableLiveData<Opinet.GasStationInfo> stnInfo;




    private MutableLiveData<List<StationEvRunnable.Item>> evStationList;
    private MutableLiveData<List<StationHydroRunnable.HydroStationObj>> hydroStationList;

    private MutableLiveData<String> gasExceptionMsg;
    private MutableLiveData<String> evExceptionMsg;
    private MutableLiveData<String> hydroExceptionMsg;

    // Get a station list stationos of which are located within a given radius conditions.
    //public MutableLiveData<List<Opinet.GasStnParcelable>> getNearStationList() {
    public MutableLiveData<List<StationGasRunnable.Item>> getNearStationList() {
        if(stnList == null) {
            stnList = new MutableLiveData<>();
        }

        return stnList;
    }

    // Get a current station which is located within MIN_RADIUS
    public MutableLiveData<StationGasRunnable.Item> getCurrentStation() {
        if(currentStation == null) {
            currentStation = new MutableLiveData<>();
        }
        return currentStation;
    }


    public MutableLiveData<SparseArray<StationInfoRunnable.Info>> getStationInfoArray() {
        if(stationInfoList == null) stationInfoList = new MutableLiveData<>();
        return stationInfoList;
    }

    public MutableLiveData<StationFavRunnable.Info> getFavStationInfo() {
        if(favStationInfo ==  null) favStationInfo = new MutableLiveData<>();
        return favStationInfo;
    }

    public MutableLiveData<List<StationEvRunnable.Item>> getEvStationList() {
        if(evStationList == null) evStationList = new MutableLiveData<>();
        return evStationList;
    }

    public MutableLiveData<List<StationHydroRunnable.HydroStationObj>> getHydroStationList() {
        if(hydroStationList == null) hydroStationList = new MutableLiveData<>();
        return hydroStationList;
    }

    public MutableLiveData<String> getEvExceptionMessage() {
        if(evExceptionMsg == null) evExceptionMsg = new MutableLiveData<>();
        return evExceptionMsg;
    }

    public MutableLiveData<String> getGasExceptionMessage() {
        if(gasExceptionMsg == null) gasExceptionMsg = new MutableLiveData<>();
        return gasExceptionMsg;
    }

    public MutableLiveData<String> getHydroExceptionMessage() {
        if(hydroExceptionMsg == null) hydroExceptionMsg = new MutableLiveData<>();
        return hydroExceptionMsg;
    }
}
