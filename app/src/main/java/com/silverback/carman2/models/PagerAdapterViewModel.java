package com.silverback.carman2.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.adapters.ExpServiceItemAdapter;
import com.silverback.carman2.adapters.ExpTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;

import java.util.List;

public class PagerAdapterViewModel extends ViewModel {

    private static final LoggingHelper log = LoggingHelperFactory.create(PagerAdapterViewModel.class);

    private MutableLiveData<ExpTabPagerAdapter> pagerAdapter;

    private MutableLiveData<ExpServiceItemAdapter> serviceAdapter;
    private MutableLiveData<JSONArray> jsonServiceArray;


    // Should conform to the Java Bean Convention when creating setter and getter.
    public MutableLiveData<ExpTabPagerAdapter> getPagerAdapter() {
        if(pagerAdapter == null) {
            pagerAdapter = new MutableLiveData<>();
            log.i("PagerAdapter in Model: %s", pagerAdapter);
        }

        return pagerAdapter;
    }

    public MutableLiveData<ExpServiceItemAdapter> getServiceAdapter() {
        if(serviceAdapter == null) {
            serviceAdapter = new MutableLiveData<>();
        }

        return serviceAdapter;
    }

    public MutableLiveData<JSONArray> getJsonServiceArray() {
        if(jsonServiceArray == null) jsonServiceArray = new MutableLiveData<>();
        return jsonServiceArray;
    }

}
