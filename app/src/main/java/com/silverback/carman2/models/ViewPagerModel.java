package com.silverback.carman2.models;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.adapters.ExpenseTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ViewPagerModel extends ViewModel {

    private static final LoggingHelper log = LoggingHelperFactory.create(ViewPagerModel.class);

    private MutableLiveData<ExpenseTabPagerAdapter> pagerAdapter;

    public MutableLiveData<ExpenseTabPagerAdapter> getPagerAdapter() {

        if(pagerAdapter == null) {
            pagerAdapter = new MutableLiveData<>();
            log.i("PagerAdapter in Model: %s", pagerAdapter);
        }

        return pagerAdapter;
    }
}
