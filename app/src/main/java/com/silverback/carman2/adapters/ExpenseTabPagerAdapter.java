package com.silverback.carman2.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceManagerFragment;
import com.silverback.carman2.fragments.StatStmtsFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class ExpenseTabPagerAdapter extends FragmentStatePagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpenseTabPagerAdapter.class);

    // Constants
    private static final int NUM_PAGES = 3;

    public ExpenseTabPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }


    private final Fragment[] fragments = new Fragment[] {
            new GasManagerFragment(),
            //new ServiceManagerFragment(),
            //new StatStmtsFragment()
            //new DummyFragment(),
            new DummyFragment(),
            new DummyFragment()

    };


    @Override
    public int getCount(){
        return fragments.length;
        //return NUM_PAGES;

    }


    @NonNull
    @Override
    public Fragment getItem(int pos) {
        return fragments[pos];
        //return new DummyFragment();
    }

    public static class DummyFragment extends Fragment {
        public DummyFragment(){}
    }

    public Fragment[] getPagerFragments() {
        return fragments;
    }

}
