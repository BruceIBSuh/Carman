package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.silverback.carman2.fragments.BillboardFragment;
import com.silverback.carman2.fragments.BillboardPopularFragment;
import com.silverback.carman2.fragments.BillboardRecentFragment;

public class BillboardPagerAdapter extends FragmentPagerAdapter {


    // Constructor
    public BillboardPagerAdapter(FragmentManager fm) {
        super(fm);
    }


    @NonNull
    @Override
    public Fragment getItem(int position) {
        return boardFragments[position];
    }

    @Override
    public int getCount() {
        return boardFragments.length;
    }

    private final Fragment[] boardFragments = new Fragment[] {
            new BillboardFragment(),
            new BillboardRecentFragment(),
            new BillboardPopularFragment()
    };
}
