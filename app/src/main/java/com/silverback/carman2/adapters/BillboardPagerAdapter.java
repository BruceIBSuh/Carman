package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.silverback.carman2.fragments.BoardAutoClubFragment;
import com.silverback.carman2.fragments.BoardInfoTipsFragment;
import com.silverback.carman2.fragments.BoardPopularFragment;
import com.silverback.carman2.fragments.BoardRecentFragment;

public class BillboardPagerAdapter extends FragmentPagerAdapter {


    // Constructor
    public BillboardPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
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
            new BoardRecentFragment(),
            new BoardPopularFragment(),
            new BoardInfoTipsFragment(),
            new BoardAutoClubFragment()
    };
}
