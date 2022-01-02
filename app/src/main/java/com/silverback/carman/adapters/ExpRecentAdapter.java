package com.silverback.carman.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.silverback.carman.fragments.ExpensePagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.util.List;

public class ExpRecentAdapter extends FragmentStateAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpRecentAdapter.class);

    // Constants
    //private static final int NUM_PAGES = 5;
    public ExpRecentAdapter(FragmentManager fm, Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        log.i("adapter position:%s", position);
        return ExpensePagerFragment.create(position);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        log.i("bindviewholder");
    }

    @Override
    public int getItemCount() {
        return Constants.NUM_RECENT_PAGES;
    }
}
