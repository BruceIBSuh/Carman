package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceManagerFragment;
import com.silverback.carman2.fragments.StatStmtsFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class CarmanFragmentPagerAdapter extends FragmentPagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CarmanFragmentPagerAdapter.class);

    // Constants
    public static final int GAS = 0;
    public static final int SERVICE = 1;
    public static final int STAT = 2;


    public CarmanFragmentPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    private final Fragment[] fragments = new Fragment[] {
            new GasManagerFragment(),
            new ServiceManagerFragment(),
            new StatStmtsFragment(),
    };

    @Override
    public int getCount(){
        return fragments.length;
    }

    @NonNull
    @Override
    public Fragment getItem(int pos) {
        return fragments[pos];
    }

}
