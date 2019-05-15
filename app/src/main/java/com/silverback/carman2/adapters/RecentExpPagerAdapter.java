package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.silverback.carman2.fragments.RecentExpPagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class RecentExpPagerAdapter extends FragmentPagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(RecentExpPagerAdapter.class);

    // Constants
    private static final int NUM_PAGES = 5;

    public RecentExpPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        log.i("position: %s", position);
        return RecentExpPagerFragment.create(position);
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }
}
