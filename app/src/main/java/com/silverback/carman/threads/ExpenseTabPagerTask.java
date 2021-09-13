package com.silverback.carman.threads;

import android.content.Context;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.PagerAdapterViewModel;

import org.json.JSONArray;

public class ExpenseTabPagerTask extends ThreadTask implements
        //ExpenseTabPagerRunnable.TabPagerMethods,
        ExpenseSvcItemsRunnable.ServiceItemsMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseTabPagerTask.class);

    // Objects
    private Context context;
    //private final ExpenseTabPagerRunnable mExpenseTabPagerRunnable;
    private final ExpenseSvcItemsRunnable mExpenseSvcItemsRunnable;
    private FragmentManager fragmentManager;
    private PagerAdapterViewModel pagerModel;
    private String[] defaults;
    private String jsonDistrict;
    private String jsonSvcItems;

    // Constructor
    ExpenseTabPagerTask() {
        super();
        //mExpenseTabPagerRunnable = new ExpenseTabPagerRunnable(context, this);
        mExpenseSvcItemsRunnable = new ExpenseSvcItemsRunnable(this);
    }

    /*
    void initPagerTask(FragmentManager fm, PagerAdapterViewModel viewModel,
                       String[] defaults, String jsonDistrict, String jsonSvcItems) {
        fragmentManager = fm;
        pagerModel = viewModel;
        this.defaults = defaults;
        this.jsonDistrict = jsonDistrict;
        this.jsonSvcItems = jsonSvcItems;
    }

     */

    void initTask(PagerAdapterViewModel pagerModel, String svcItems) {
        this.pagerModel = pagerModel;
        this.jsonSvcItems = svcItems;
    }
    /*
    Runnable getTabPagerRunnable() {
        return mExpenseTabPagerRunnable;
    }

     */
    Runnable getServiceItemsRunnable() {
        return mExpenseSvcItemsRunnable;
    }

    /*
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

     */

    @Override
    public String getJsonServiceItems() {
        return jsonSvcItems;
    }

    /*
    @Override
    public void setTabPagerTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

     */

    @Override
    public void setServiceItemsThread(Thread thread) {
        setCurrentLocation(thread);
    }


//    @Override
//    public void setTabPagerAdapter(ExpTabPagerAdapter adapter) {
//        pagerModel.getPagerAdapter().postValue(adapter);
//    }

    @Override
    public void setJsonSvcArray(JSONArray jsonArray) {
        pagerModel.getJsonServiceArray().postValue(jsonArray);
    }


    @Override
    public void handleSvcItemsTask(int state) {
        int outstate = -1;
        switch(state) {
            case ExpenseSvcItemsRunnable.TASK_COMPLETE:
                break;
            case ExpenseSvcItemsRunnable.TASK_FAIL:
                break;
        }

        sThreadManager.handleState(this, outstate);
    }


}
