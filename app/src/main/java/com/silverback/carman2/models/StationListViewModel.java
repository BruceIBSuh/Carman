package com.silverback.carman2.models;

import android.util.SparseArray;
import android.util.SparseBooleanArray;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class StationListViewModel extends ViewModel {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationListViewModel.class);

    // Objects
    private MutableLiveData<List<Opinet.GasStnParcelable>> stnList;
    private MutableLiveData<Opinet.GasStnParcelable> currentStation;
    private MutableLiveData<Opinet.GasStationInfo> stnInfo;
    private MutableLiveData<SparseBooleanArray> hasCarWash;

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



}
