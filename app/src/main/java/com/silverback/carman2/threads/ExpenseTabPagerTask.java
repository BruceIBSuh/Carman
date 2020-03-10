package com.silverback.carman2.threads;

import android.content.Context;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman2.adapters.ExpTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.PagerAdapterViewModel;

import org.json.JSONArray;

public class ExpenseTabPagerTask extends ThreadTask implements
        ExpenseTabPagerRunnable.TabPagerMethods, ServiceItemsRunnable.ServiceItemsMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseTabPagerTask.class);

    // Objects
    private Context context;
    private ExpenseTabPagerRunnable mExpenseTabPagerRunnable;
    private ServiceItemsRunnable mServiceItemsRunnable;
    private FragmentManager fragmentManager;
    private PagerAdapterViewModel pagerModel;
    private String[] defaults;
    private String jsonDistrict;
    private String jsonSvcItems;

    // Constructor
    ExpenseTabPagerTask(Context context) {
        super();
        mExpenseTabPagerRunnable = new ExpenseTabPagerRunnable(context, this);
        mServiceItemsRunnable = new ServiceItemsRunnable(this);
    }

    void initPagerTask(FragmentManager fm, PagerAdapterViewModel viewModel,
                       String[] defaults, String jsonDistrict, String jsonSvcItems) {

        fragmentManager = fm;
        pagerModel = viewModel;
        this.defaults = defaults;
        this.jsonDistrict = jsonDistrict;
        this.jsonSvcItems = jsonSvcItems;
    }

    Runnable getTabPagerRunnable() {
        return mExpenseTabPagerRunnable;
    }
    Runnable getServiceItemsRunnable() {
        return mServiceItemsRunnable;
    }

    @Override
    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    @Override
    public String[] getDefaults() {
        return defaults;
    }

    @Override
    public String getJsonDistrict() {
        return jsonDistrict;
    }

    @Override
    public String getJsonServiceItems() {
        return jsonSvcItems;
    }


    @Override
    public void setTabPagerTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setServiceItemsThread(Thread thread) {
        setCurrentThread(thread);
    }


    @Override
    public void setTabPagerAdapter(ExpTabPagerAdapter adapter) {
        log.i("Task TabPagerAdapter: %s", adapter);
        pagerModel.getPagerAdapter().postValue(adapter);
    }

    @Override
    public void setJsonSvcArray(JSONArray jsonArray) {
        pagerModel.getJsonServiceArray().postValue(jsonArray);
    }


    @Override
    public void handleRecyclerTask(int state) {

    }


}
