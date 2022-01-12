package com.silverback.carman.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.silverback.carman.fragments.ExpenseGasFragment;
import com.silverback.carman.fragments.ExpenseServiceFragment;
import com.silverback.carman.fragments.ExpenseStmtsFragment;

import java.lang.ref.WeakReference;

// Adapter for ViewPager2 placed below the top frame, which displays ExpenseGasFragment,
// ExpenseServiceFragment, and ExpenseGraphFragment in order.

public class ExpensePagerAdapter extends FragmentStateAdapter {
    //private static final LoggingHelper log = LoggingHelperFactory.create(ExpensePagerAdapter.class);

    public ExpensePagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
        super(fm, lifecycle);
    }
    //private WeakReference<Fragment> weakFragmentReference;

    private final Fragment[] fragments = new Fragment[]{
            new ExpenseGasFragment(),
            new ExpenseServiceFragment(),
            new ExpenseStmtsFragment()
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

    public Fragment getCurrentFragment(int position) {
        return fragments[position];
    }

    public WeakReference<Fragment> weakFragmentReference(int position) {
        return new WeakReference<>(fragments[position]);
    }
}
