package com.silverback.carman.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

public class SpinnerDistrictModel extends ViewModel {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SpinnerDistrictModel.class);

    private MutableLiveData<List<Opinet.DistrictCode>> spinnerList;

    public MutableLiveData<List<Opinet.DistrictCode>> getSpinnerDataList() {
        if(spinnerList == null) spinnerList = new MutableLiveData<>();
        return spinnerList;
    }
}
