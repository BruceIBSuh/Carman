package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.silverback.carman2.fragments.BoardPagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class BoardPagerAdapter extends FragmentPagerAdapter {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerAdapter.class);
    private static final int NUM_PAGES = 4;

    // Objects

    // Constructor
    public BoardPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return BoardPagerFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }

}
