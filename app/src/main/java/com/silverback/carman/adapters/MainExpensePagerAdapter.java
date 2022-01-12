package com.silverback.carman.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.silverback.carman.fragments.MainExpensePagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

public class MainExpensePagerAdapter extends FragmentStateAdapter {
    private static final LoggingHelper log = LoggingHelperFactory.create(MainExpensePagerAdapter.class);
    private static final int NUM_PAGES = 2;

    private MainExpensePagerFragment targetFragment;

    public MainExpensePagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return targetFragment = MainExpensePagerFragment.newInstance(position);
    }


    @Override
    public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            log.i("payloads exist");
        }

    }
}
