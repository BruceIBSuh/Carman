package com.silverback.carman2.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.adapters.ExpenseSvcRecyclerAdapter;
import com.silverback.carman2.adapters.ExpenseTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class AdapterViewModel extends ViewModel {

    private static final LoggingHelper log = LoggingHelperFactory.create(AdapterViewModel.class);

    private MutableLiveData<ExpenseTabPagerAdapter> pagerAdapter;
    private MutableLiveData<ExpenseSvcRecyclerAdapter> serviceAdapter;
    private MutableLiveData<List<String>> servicedItem;

    // Should conform to the Java Bean Convention when creating setter and getter.
    public MutableLiveData<ExpenseTabPagerAdapter> getPagerAdapter() {
        if(pagerAdapter == null) {
            pagerAdapter = new MutableLiveData<>();
            log.i("PagerAdapter in Model: %s", pagerAdapter);
        }

        return pagerAdapter;
    }

    public MutableLiveData<ExpenseSvcRecyclerAdapter> getServiceAdapter() {
        if(serviceAdapter == null) {
            serviceAdapter = new MutableLiveData<>();
        }

        return serviceAdapter;
    }

    public MutableLiveData<List<String>> getServicedItem() {
        if(servicedItem == null) {
            servicedItem = new MutableLiveData<>();
        }

        return servicedItem;
    }

}
