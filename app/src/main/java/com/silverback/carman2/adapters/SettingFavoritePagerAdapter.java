package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.silverback.carman2.fragments.FavoriteGasPagerFragment;
import com.silverback.carman2.fragments.FavoriteServicePagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class SettingFavoritePagerAdapter extends FragmentPagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoritePagerAdapter.class);


    // Constructor
    public SettingFavoritePagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragment[position];
    }

    @Override
    public int getCount() {
        return fragment.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return (position == 0)? "Gas Station" : "ServiceStation";
    }


    private final Fragment[] fragment = new Fragment[] {
            new FavoriteGasPagerFragment(),
            new FavoriteServicePagerFragment()
    };
}
