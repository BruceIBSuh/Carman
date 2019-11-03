package com.silverback.carman2.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.Map;

public class ServiceCenterViewModel extends ViewModel {

    private static final LoggingHelper log = LoggingHelperFactory.create(ServiceCenterViewModel.class);

    private MutableLiveData<Map<String, Object>> currentSVC;

    public MutableLiveData<Map<String, Object>> getCurrentSVC() {
        if(currentSVC == null) currentSVC = new MutableLiveData<>();
        return currentSVC;
    }
}
