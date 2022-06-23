package com.silverback.carman.viewmodels;

import android.util.SparseBooleanArray;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.rest.EvRetrofitTikXml;
import com.silverback.carman.threads.StationEvRunnable;
import com.silverback.carman.threads.StationHydroRunnable;

import java.util.List;

public class StationListViewModel extends ViewModel {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListViewModel.class);

    // Objects
    private MutableLiveData<List<Opinet.GasStnParcelable>> stnList;
    private MutableLiveData<Opinet.GasStnParcelable> currentStation;
    private MutableLiveData<Opinet.GasStationInfo> stnInfo;
    private MutableLiveData<SparseBooleanArray> hasCarWash;


    private MutableLiveData<List<StationEvRunnable.Item>> evStationList;
    private MutableLiveData<List<StationHydroRunnable.HydroStationObj>> hydroStationList;

    private MutableLiveData<String> exceptionMessage;

    // Get a station list stationos of which are located within a given radius conditions.
    public MutableLiveData<List<Opinet.GasStnParcelable>> getNearStationList() {
        if(stnList == null) {
            stnList = new MutableLiveData<>();
        }

        return stnList;
    }

    // Get a current station which is located within MIN_RADIUS
    public MutableLiveData<Opinet.GasStnParcelable> getCurrentStation() {
        if(currentStation == null) {
            currentStation = new MutableLiveData<>();
        }
        return currentStation;
    }

    public MutableLiveData<SparseBooleanArray> getStationCarWashInfo() {
        log.i("getStationCarWashInfo");
        if(hasCarWash == null) {
            hasCarWash = new MutableLiveData<>();
        }
        return hasCarWash;
    }

    public MutableLiveData<List<StationEvRunnable.Item>> getEvStationList() {
        if(evStationList == null) evStationList = new MutableLiveData<>();
        return evStationList;
    }

    public MutableLiveData<List<StationHydroRunnable.HydroStationObj>> getHydroStationList() {
        if(hydroStationList == null) hydroStationList = new MutableLiveData<>();
        return hydroStationList;
    }

    public MutableLiveData<String> getExceptionMessage() {
        if(exceptionMessage == null) exceptionMessage = new MutableLiveData<>();
        return exceptionMessage;
    }

}
