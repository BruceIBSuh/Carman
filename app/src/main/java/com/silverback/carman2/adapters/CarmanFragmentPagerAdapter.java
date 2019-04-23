package com.silverback.carman2.adapters;

import android.os.Bundle;

import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceFragment;
import com.silverback.carman2.fragments.StatFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class CarmanFragmentPagerAdapter extends FragmentPagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CarmanFragmentPagerAdapter.class);

    // Constants
    private static final int GAS = 0;
    private static final int SERVICE = 1;
    private static final int STAT = 2;


    // Objects
    private String json;

    public CarmanFragmentPagerAdapter(FragmentManager fm, String json) {
        super(fm);
        fm.beginTransaction().addToBackStack(null);
        this.json = json;

    }

    private final Fragment[] fragments = new Fragment[] {
            new GasManagerFragment(),
            new ServiceFragment(),
            new StatFragment(),
    };

    @Override
    public int getCount(){
        return fragments.length;
    }

    @NonNull
    @Override
    public Fragment getItem(int pos){
        switch(pos) {
            case GAS:
                log.i("Fragment: %s", pos);
                break;

            case SERVICE:
                log.i("Fragment: %s", pos);
                Bundle bundle = new Bundle();
                bundle.putString("serviceItems", json);
                fragments[pos].setArguments(bundle);
                break;

            case STAT:
                log.i("Fragment: %s", pos);
                break;

        }

        return fragments[pos];
    }

}
