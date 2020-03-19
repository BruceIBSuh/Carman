package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.silverback.carman2.fragments.BoardPagerFragment;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/*
 * This viewpager adapter subclasses FragmentStatePagerAdapter instead of FragmentPagerAdapter.
 * In general, FragmentPagerAdapter is preferred when displaying the tab-synced fragments that do not
 * contain lots of heavy data. However, displaying not a few items with bitmaps may burden the adapter.
 * For this reason, in spite of the tab-working adapter, FragmentStatePagerAdpater is applied.
 */
public class BoardPagerAdapter extends FragmentStatePagerAdapter {

    private static final int NUM_PAGES = 4;

    // Fields
    private String autoFilter;
    private boolean[] cbValues;

    // Constructor
    public BoardPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return (position == Constants.BOARD_AUTOCLUB)?
                BoardPagerFragment.newInstance(position, autoFilter, cbValues) :
                BoardPagerFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }


    public void setCheckBoxValues(String jsonFilterName, boolean[] values) {
        autoFilter = jsonFilterName;
        cbValues = values;
    }

}
