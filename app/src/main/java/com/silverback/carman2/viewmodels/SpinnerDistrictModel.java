package com.silverback.carman2.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

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
