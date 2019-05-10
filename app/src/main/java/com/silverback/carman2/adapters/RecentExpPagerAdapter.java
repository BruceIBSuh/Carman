package com.silverback.carman2.adapters;

import com.silverback.carman2.fragments.RecentExpPagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class RecentExpPagerAdapter extends FragmentStatePagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(RecentExpPagerAdapter.class);

    private int mPage;

    public RecentExpPagerAdapter(FragmentManager fm, int page) {
        super(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mPage = page;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        log.i("position: %s", position);
        return RecentExpPagerFragment.create(position);
    }

    @Override
    public int getCount() {
        return mPage;
    }
}
