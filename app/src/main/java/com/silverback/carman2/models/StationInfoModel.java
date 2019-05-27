package com.silverback.carman2.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;


public class StationInfoModel extends ViewModel {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationInfoModel.class);

    private MutableLiveData<String> stnAddrs;
    public LiveData<String> getAddress() {
        if(stnAddrs == null) {
            stnAddrs = new MutableLiveData<>();
        }

        return stnAddrs;
    }

    public void setAddress(String addrs) {
        stnAddrs.setValue(addrs);
    }


}
