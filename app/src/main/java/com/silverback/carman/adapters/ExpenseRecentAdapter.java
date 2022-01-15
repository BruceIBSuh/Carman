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

public class ExpenseRecentAdapter extends FragmentStateAdapter{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseRecentAdapter.class);

    private ExpensePagerFragment expensePagerFragment;
    private int index;

    public ExpenseRecentAdapter(FragmentManager fm, Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        expensePagerFragment = ExpensePagerFragment.getInstance(position);
        return expensePagerFragment;
    }

    @Override
    public void onBindViewHolder(
            @NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else expensePagerFragment.dispRecentExpensePager(index, position);
    }

    @Override
    public int getItemCount() {
        return Constants.NUM_RECENT_PAGES;
    }

    public void setCurrentFragment(int index) {
        this.index = index;
    }

}
