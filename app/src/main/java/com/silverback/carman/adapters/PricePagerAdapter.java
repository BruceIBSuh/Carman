package com.silverback.carman.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.adapter.FragmentViewHolder;

import com.silverback.carman.fragments.PricePagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.List;

/*
 * This class is a viewpager adapter that displays the gas price of a region and a station set as
 * the place of interest in each page, the fragment of which is defined in PricePagerFragment as
 * a single fragment with different values.
 *
 * FragmentStatePagerAdapter destroy the entire fragment when it is not visible, keeping the saved
 * state of the fragment, thus use much less memory compared with FragmentPagerAdapter.
 *
 * The apdater may react when the spinner selects a  new fuel code or users sets or changes the
 * first-set favorite gas station in PreferenceActivity, which is notified by favoritePriceComplete()
 * of OpinetViewModel in GeneralFragment.
 */
//public class PricePagerAdapter extends FragmentStatePagerAdapter {
public class PricePagerAdapter extends FragmentStateAdapter {
    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(PricePagerAdapter.class);
    private static final int NUM_PAGES = 2;

    // Objects
    private PricePagerFragment distFragment;
    private PricePagerFragment stnFragment;
    private String fuelCode;


    // Constructor
    public PricePagerAdapter(FragmentActivity fa){
        super(fa);
    }

    // Invoked when the spinner changes the value or users reset the top priority gas station.
    public void setFuelCode(String fuelCode) {
        this.fuelCode = fuelCode;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 0) return distFragment = PricePagerFragment.getInstance(fuelCode, 0);
        else if(position == 1) return stnFragment = PricePagerFragment.getInstance(fuelCode, 1);
        else return PricePagerFragment.getInstance(fuelCode, position);

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
