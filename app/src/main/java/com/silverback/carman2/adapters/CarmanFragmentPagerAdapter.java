package com.silverback.carman2.adapters;

import android.app.Activity;
import android.content.Context;

import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceFragment;
import com.silverback.carman2.fragments.StatStmtsFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class CarmanFragmentPagerAdapter extends FragmentPagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CarmanFragmentPagerAdapter.class);

    // Constants
    private static final int GAS = 0;
    private static final int SERVICE = 1;
    private static final int STAT = 2;


    // Objects
    private String json;
    private Context context;

    public CarmanFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    private final Fragment[] fragments = new Fragment[] {
            new GasManagerFragment(),
            new ServiceFragment(),
            new StatStmtsFragment(),
    };

    @Override
    public int getCount(){
        return fragments.length;
    }

    @NonNull
    @Override
    public Fragment getItem(int pos){
        return fragments[pos];
    }

}
