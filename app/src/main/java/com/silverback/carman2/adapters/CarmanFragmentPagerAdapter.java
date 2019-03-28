package com.silverback.carman2.adapters;

import com.silverback.carman2.fragments.GasFragment;
import com.silverback.carman2.fragments.ServiceFragment;
import com.silverback.carman2.fragments.SettingFragment;
import com.silverback.carman2.fragments.StatFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class CarmanFragmentPagerAdapter extends FragmentPagerAdapter {


    public CarmanFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    private final Fragment[] fragments = new Fragment[] {
            new GasFragment(),
            new ServiceFragment(),
            new StatFragment(),
            new SettingFragment()
    };

    @Override
    public int getCount(){
        return fragments.length;
    }

    @Override
    public Fragment getItem(int pos){
        return fragments[pos];
    }

}
