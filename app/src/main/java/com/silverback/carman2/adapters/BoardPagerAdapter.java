package com.silverback.carman2.adapters;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.silverback.carman2.fragments.BoardPagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import java.util.ArrayList;

/*
 * This viewpager adapter subclasses FragmentStatePagerAdapter instead of FragmentPagerAdapter.
 * In general, FragmentPagerAdapter is preferred when displaying the tab-synced fragments that do not
 * contain lots of heavy data. However, displaying not a few items with bitmaps may burden the adapter.
 * For this reason, in spite of the tab-working adapter, FragmentStatePagerAdpater is preferred here.
 */
public class BoardPagerAdapter extends FragmentStatePagerAdapter {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerAdapter.class);
    private static final int NUM_PAGES = 4;

    // Objects
    private ArrayList<String> cbValues;
    private int currentPage;

    // Constructor
    public BoardPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        log.i("Fragment page: %s", position);
        currentPage = position;
        //String automaker = (cbValues.size() > 0) ? cbValues.get(0) : null;
        return BoardPagerFragment.newInstance(position, cbValues);
        /*
        return (position == Constants.BOARD_AUTOCLUB)?
                BoardPagerFragment.newInstance(position, cbValues) :
                BoardPagerFragment.newInstance(position);
        */
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }

    // As long as the current page is AUTO_CLUB, update the viewpager adapter by setting the return
    // type as POSITION_NONE. It invokes destroyItem() and regards the current fragment destroyed
    // which leads to call onCreateView() of the fragment.


    @Override
    public int getItemPosition(@NonNull Object object) {
        log.i("getItemPosition: %s", object);
        //if(object instanceof BoardPagerFragment) return POSITION_NONE;
        //else return POSITION_UNCHANGED;
        return POSITION_UNCHANGED;
    }

    public void setAutoFilterValues(ArrayList<String> values) {
        cbValues = values;
    }
}
