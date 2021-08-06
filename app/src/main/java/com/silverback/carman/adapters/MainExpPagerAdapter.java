package com.silverback.carman.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.silverback.carman.fragments.MainExpPagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class MainExpPagerAdapter extends FragmentStateAdapter {
    private static final LoggingHelper log = LoggingHelperFactory.create(MainExpPagerAdapter.class);
    private static final int NUM_PAGES = 3;

    public MainExpPagerAdapter(FragmentActivity fa) {
        super(fa);
    }


    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return MainExpPagerFragment.newInstance(position);
    }
}
