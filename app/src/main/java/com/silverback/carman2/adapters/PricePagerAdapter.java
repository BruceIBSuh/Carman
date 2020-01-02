package com.silverback.carman2.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.silverback.carman2.fragments.PricePagerFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * This class shows the gas price of the region(Sido and Sigun) and the favorite station in each
 * page of ViewPager in GeneralFragment of MainActivity, using the single type of fragment
 * (PricePagerFragment) with different values.
 *
 * FragmentStatePagerAdapter destroy the entire fragment when it is not visible, keeping the saved
 * state of the fragment, thus use much less memory compared with FragmentPagerAdapter.
 *
 * The apdater may react when the spinner selects a  new fuel code or users sets or changes the
 * first-set favorite gas station in PreferenceActivity, which is notified by favoritePriceComplete()
 * of OpinetViewModel in GeneralFragment.
 */
public class PricePagerAdapter extends FragmentStatePagerAdapter {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(PricePagerAdapter.class);
    private static final int NUM_PAGES = 2;

    // Objects
    private String fuelCode;

    // Constructor
    public PricePagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return PricePagerFragment.getInstance(fuelCode, position);
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }

    // Invoked when the spinner changes the value or users reset the top priority gas station.
    public void setFuelCode(String fuelCode) {
        this.fuelCode = fuelCode;
        log.i("Fuel Code: %s", fuelCode);
    }
}
