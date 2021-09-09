package com.silverback.carman.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.silverback.carman.fragments.GasManagerFragment;
import com.silverback.carman.fragments.ServiceManagerFragment;
import com.silverback.carman.fragments.StatStmtsFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

// Adapter for ViewPager2 placed below the top frame, which displays GasManagerFragment,
// ServiceManagerFragment, and StatGraphFragment in order.

public class ExpContentPagerAdapter extends FragmentStateAdapter {
    //private static final LoggingHelper log = LoggingHelperFactory.create(ExpContentPagerAdapter.class);

    public ExpContentPagerAdapter(FragmentActivity fa) {
        super(fa);
    }
    public ExpContentPagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    private final Fragment[] fragments = new Fragment[]{
            new GasManagerFragment(),
            new ServiceManagerFragment(),
            new StatStmtsFragment()
    };

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments.length;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
    public Fragment getCurrentFragment(int position) {
        return fragments[position];
    }

     */
}
