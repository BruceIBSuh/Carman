package com.silverback.carman2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.silverback.carman2.R;
import com.silverback.carman2.fragments.ExpensePagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

public class ExpRecentPagerAdapter extends FragmentStatePagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ExpRecentPagerAdapter.class);

    // Constants
    //private static final int NUM_PAGES = 5;


    public ExpRecentPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }


    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        return super.instantiateItem(container, position);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.destroyItem(container, position, object);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return ExpensePagerFragment.create(position);
    }

    @Override
    public int getCount() {
        return Constants.NUM_RECENT_PAGES;
    }
}
