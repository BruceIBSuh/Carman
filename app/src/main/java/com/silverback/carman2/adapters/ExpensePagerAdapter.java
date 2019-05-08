package com.silverback.carman2.adapters;

import com.silverback.carman2.fragments.RecentExpensePageFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class ExpensePagerAdapter extends FragmentStatePagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpensePagerAdapter.class);

    private int mPage;

    public ExpensePagerAdapter(FragmentManager fm, int page) {
        super(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mPage = page;
    }

    @Override
    public Fragment getItem(int position) {
        log.i("position: %s", position);
        return RecentExpensePageFragment.create(position);
    }

    @Override
    public int getCount() {
        return mPage;
    }
}
