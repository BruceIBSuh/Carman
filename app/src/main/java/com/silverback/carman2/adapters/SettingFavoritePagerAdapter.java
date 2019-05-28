package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.silverback.carman2.fragments.FavoriteGasFragment;
import com.silverback.carman2.fragments.FavoriteServiceFragment;

public class SettingFavoritePagerAdapter extends FragmentPagerAdapter {

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
            new FavoriteGasFragment(),
            new FavoriteServiceFragment()
    };
}
