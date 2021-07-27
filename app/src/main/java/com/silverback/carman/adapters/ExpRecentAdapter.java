package com.silverback.carman.adapters;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.silverback.carman.fragments.ExpensePagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

//public class ExpRecentAdapter extends FragmentStatePagerAdapter {
public class ExpRecentAdapter extends FragmentStateAdapter {


    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpRecentAdapter.class);

    // Constants
    //private static final int NUM_PAGES = 5;


    public ExpRecentAdapter(FragmentManager fm, Lifecycle lifecycle) {
        //super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        super(fm, lifecycle);
    }


//    @NonNull
//    @Override
//    public Object instantiateItem(@NonNull ViewGroup container, int position) {
//        return super.instantiateItem(container, position);
//    }
//
//    @Override
//    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
//        super.destroyItem(container, position, object);
//    }
//
//    @NonNull
//    @Override
//    public Fragment getItem(int position) {
//        return ExpensePagerFragment.create(position);
//    }
//
//    @Override
//    public int getCount() {
//        return Constants.NUM_RECENT_PAGES;
//    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return ExpensePagerFragment.create(position);
    }

    @Override
    public int getItemCount() {
        return Constants.NUM_RECENT_PAGES;
    }
}