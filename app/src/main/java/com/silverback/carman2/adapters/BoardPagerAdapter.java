package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.silverback.carman2.fragments.BoardPagerFragment;

/**
 * This viewpager adapter subclasses FragmentStatePagerAdapter instead of FragmentPagerAdapter.
 * In general, FragmentPagerAdapter is preferred when displaying the tab-synced fragments that do not
 * contain lots of heavy data. However, displaying not a few posting items with bitmaps may burden
 * the adapter. For this reason, in spite of the tab-working adapter, FragmentStatePagerAdpater is
 * applied.
 */
public class BoardPagerAdapter extends FragmentStatePagerAdapter {

    private static final int NUM_PAGES = 4;

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
