package com.silverback.carman.adapters;

import static com.silverback.carman.BoardActivity.AUTOCLUB;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.silverback.carman.fragments.BoardPagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

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
    private ArrayList<String> cbValues;
    private int position;
    private boolean isAutoClub;

    public BoardPagerAdapter(@NonNull FragmentManager fm, @NonNull Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == AUTOCLUB)  isAutoClub = true;
        return BoardPagerFragment.newInstance(position, cbValues);
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

    public void setAutoFilterValues(ArrayList<String> values) {
        cbValues = values;
    }
}
