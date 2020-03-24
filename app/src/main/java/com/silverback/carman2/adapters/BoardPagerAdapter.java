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

import static com.silverback.carman2.backgrounds.GeofenceResetService.log;

/*
 * This viewpager adapter subclasses FragmentStatePagerAdapter instead of FragmentPagerAdapter.
 * In general, FragmentPagerAdapter is preferred when displaying the tab-synced fragments that do not
 * contain lots of heavy data. However, displaying not a few items with bitmaps may burden the adapter.
 * For this reason, in spite of the tab-working adapter, FragmentStatePagerAdpater is applied.
 */
public class BoardPagerAdapter extends FragmentStatePagerAdapter {

    private static final int NUM_PAGES = 4;

    // Objects
    private ArrayList<CharSequence> cbValues;
    private int currentPage;

    // Constructor
    public BoardPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        log.i("fragment position: %s", position);
        currentPage = position;
        return (position == Constants.BOARD_AUTOCLUB)?
                BoardPagerFragment.newInstance(position, cbValues) :
                BoardPagerFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }

    // As long as the current page is AUTO_CLUB, update the viewpager adapter which should
    // set the return type with POSITION_NONE.
    @Override
    public int getItemPosition(@NonNull Object object) {

        if(currentPage == Constants.BOARD_AUTOCLUB) return POSITION_NONE;
        else return POSITION_UNCHANGED;
    }


    public void setAutoFilterValues(ArrayList<CharSequence> values) {
        for(CharSequence value : values) log.i("filter value: %s", value);
        cbValues = values;

    }
}
