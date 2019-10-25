package com.silverback.carman2.threads;

import androidx.fragment.app.FragmentManager;

import com.silverback.carman2.adapters.ExpTabPagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.PagerAdapterViewModel;

public class TabPagerTask extends ThreadTask implements TabPagerRunnable.ViewPagerMethods {

    private static final LoggingHelper log = LoggingHelperFactory.create(TabPagerTask.class);

    // Objects
    private FragmentManager fragmentManager;
    private PagerAdapterViewModel pagerModel;
    private String[] defaults;
    private String jsonDistrict;
    private String userId;

    // Constructor
    TabPagerTask() {
        super();
    }

    void initViewPagerTask(
            FragmentManager fm,
            PagerAdapterViewModel viewModel,
            String[] defaults, String json, String userId) {

        fragmentManager = fm;
        pagerModel = viewModel;
        this.defaults = defaults;
        jsonDistrict = json;
        this.userId = userId;
    }

    Runnable getViewPagerRunnable() {
        return new TabPagerRunnable(this);
    }


    @Override
    public void setViewPagerTaskThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setViewPagerAdapter(ExpTabPagerAdapter adapter) {
        pagerModel.getPagerAdapter().postValue(adapter);
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
    public String getUserId() {
        return userId;
    }


}
