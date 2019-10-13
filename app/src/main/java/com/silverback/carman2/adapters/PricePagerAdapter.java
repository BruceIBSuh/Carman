package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.silverback.carman2.fragments.PricePagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.views.OpinetSidoPriceView;

public class PricePagerAdapter extends FragmentStatePagerAdapter {

    private static final LoggingHelper log = LoggingHelperFactory.create(PricePagerAdapter.class);

    private static final int NUM_PAGES = 2;
    private String fuelCode;

    // Constructor
    public PricePagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return PricePagerFragment.getInstance(fuelCode, position);
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }

    public void setFuelCode(String fuelCode) {
        this.fuelCode = fuelCode;
        log.i("Fuel Code: %s", fuelCode);
    }
}
