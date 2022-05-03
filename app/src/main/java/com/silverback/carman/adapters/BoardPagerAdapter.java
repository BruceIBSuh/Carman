package com.silverback.carman.adapters;

import static com.silverback.carman.BoardActivity.AUTOCLUB;
import static com.silverback.carman.BoardActivity.NUM_PAGES;

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

    // Objects
    private final ArrayList<String> autofilter;
    private final List<BoardPagerFragment> fragmentList;

    public BoardPagerAdapter(
            @NonNull FragmentManager fm, @NonNull Lifecycle lifecycle, ArrayList<String> autofilter) {
        super(fm, lifecycle);
        this.autofilter = autofilter;
        fragmentList = new ArrayList<>();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        BoardPagerFragment fragment = BoardPagerFragment.newInstance(position, autofilter);
        fragmentList.add(fragment);
        return fragmentList.get(position);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            for(Object payload : payloads) {
                if(payload instanceof ArrayList<?>) {
                    ArrayList<?> tempList = (ArrayList<?>)payload;
                    ArrayList<String> autofilter = new ArrayList<>();
                    for(Object obj : tempList) autofilter.add((String)obj);
                    //autoFragment.updateAutoFilter(autofilter);
                    fragmentList.get(AUTOCLUB).resetAutoFilter(autofilter);
                    break;
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public boolean containsItem(long itemId) {
        return false;
    }
}
