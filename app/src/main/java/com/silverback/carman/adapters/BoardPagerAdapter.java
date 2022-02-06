package com.silverback.carman.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.silverback.carman.fragments.BoardPagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

/*
 * REFACTOR: FragmentStatePagerAdapter(FragmentPagerAdapter) to FragmentStateAdapter
 * Extends FragmentStateAdapter abstract class, implementing the createFragment() to supply instances
 * of fragments therein as new pages and getitemCount()
 */


public class BoardPagerAdapter extends FragmentStateAdapter {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerAdapter.class);
    private static final int NUM_PAGES = 4;

    // Objects
    private BoardPagerFragment mFragment;
    private final ArrayList<String> autofilter;
    //private int position;
    //private boolean isAutoClub;

    public BoardPagerAdapter(
            @NonNull FragmentManager fm, @NonNull Lifecycle lifecycle, ArrayList<String> autofilter) {
        super(fm, lifecycle);
        this.autofilter = autofilter;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        //if(position == AUTOCLUB)  isAutoClub = true;
        mFragment = BoardPagerFragment.newInstance(position, autofilter);
        return mFragment;
    }

    @Override
    public void onBindViewHolder(
            @NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else log.i("update postpager:%s", payloads);
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
    // Referenced from onCheckedChanged() in BoardActivity to get the current fragment.
    public BoardPagerFragment getPagerFragment() {
        return mFragment;
    }
}
