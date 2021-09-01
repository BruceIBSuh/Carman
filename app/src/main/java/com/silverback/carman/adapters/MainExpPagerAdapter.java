package com.silverback.carman.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.silverback.carman.fragments.MainContentPagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

public class MainExpPagerAdapter extends FragmentStateAdapter {
    private static final LoggingHelper log = LoggingHelperFactory.create(MainExpPagerAdapter.class);
    private static final int NUM_PAGES = 2;

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
        return MainContentPagerFragment.newInstance(position);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {

        super.onBindViewHolder(holder, position, payloads);
        log.i("payloads: %s", payloads);
    }
}
