package com.silverback.carman.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.silverback.carman.fragments.MainPricePagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

/*
 * This class is a viewpager adapter that displays the gas price of a region and a station set as
 * the place of interest in each page, the fragment of which is defined in MainPricePagerFragment as
 * a single fragment with different values.
 *
 * FragmentStatePagerAdapter destroy the entire fragment when it is not visible, keeping the saved
 * state of the fragment, thus use much less memory compared with FragmentPagerAdapter.
 *
 * The apdater may react when the spinner selects a  new fuel code or users sets or changes the
 * first-set favorite gas station in PreferenceActivity, which is notified by favoritePriceComplete()
 * of OpinetViewModel in GeneralFragment.
 */
//public class MainPricePagerAdapter extends FragmentStatePagerAdapter {
public class MainPricePagerAdapter extends FragmentStateAdapter {
    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(MainPricePagerAdapter.class);
    private static final int NUM_PAGES = 2;

    // Objects
    private MainPricePagerFragment distFragment;
    private MainPricePagerFragment stnFragment;
    private String fuelCode;


    // Constructor
    public MainPricePagerAdapter(FragmentActivity fa){
        super(fa);
    }

    // Invoked when the spinner changes the value or users reset the top priority gas station.
    public void setFuelCode(String fuelCode) {
        this.fuelCode = fuelCode;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 0) return distFragment = MainPricePagerFragment.getInstance(fuelCode, 0);
        else if(position == 1) return stnFragment = MainPricePagerFragment.getInstance(fuelCode, 1);
        else return MainPricePagerFragment.getInstance(fuelCode, position);

    }

    @Override
    public void onBindViewHolder(
            @NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads){

        super.onBindViewHolder(holder, position, payloads);
        log.i("pricepageradapter onbindviewholder:%s, %s", holder, payloads.size());

        if(position == 0) distFragment.reload(0, fuelCode);
        else if(position == 1) stnFragment.reload(1, fuelCode);
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }

}
