package com.silverback.carman2.models;

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

    // Get a station list stationos of which are located within a given radius conditions.
    public MutableLiveData<List<Opinet.GasStnParcelable>> getStationListLiveData() {
        if(stnList == null) {
            stnList = new MutableLiveData<>();
        }

        return stnList;
    }

    // Get a current station which is located within MIN_RADIUS
    public MutableLiveData<Opinet.GasStnParcelable> getCurrentStationLiveData() {
        if(currentStation == null) {
            currentStation = new MutableLiveData<>();
        }
        return currentStation;
    }

    // Retrieve the information of an station picked by clicking an item view of RecyclerView
    public MutableLiveData<Opinet.GasStationInfo> getStationInfoLiveData() {
        if(stnInfo == null) {
            stnInfo = new MutableLiveData<>();
        }

        return stnInfo;
    }
}
