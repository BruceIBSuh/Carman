package com.silverback.carman2.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.fragments.GasManagerFragment;
import com.silverback.carman2.fragments.ServiceFragment;
import com.silverback.carman2.fragments.StatStmtsFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;

import org.json.JSONArray;

import java.text.DecimalFormat;
import java.util.Arrays;

import static com.silverback.carman2.BaseActivity.getDecimalFormatInstance;

public class CarmanFragmentPagerAdapter extends FragmentPagerAdapter {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CarmanFragmentPagerAdapter.class);

    // Constants
    public static final int GAS = 0;
    public static final int SERVICE = 1;
    public static final int STAT = 2;


    // Objects
    private Context context;
    private SharedPreferences mSettings;
    private DecimalFormat df;
    private String json;

    public CarmanFragmentPagerAdapter(Context context, FragmentManager fm) {
        super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT); // bug?

        this.context = context;
        mSettings = BaseActivity.getSharedPreferenceInstance(context);
        df = BaseActivity.getDecimalFormatInstance();

    }

    private final Fragment[] fragments = new Fragment[] {
            new GasManagerFragment(),
            new ServiceFragment(),
            new StatStmtsFragment(),
    };

    @Override
    public int getCount(){
        return fragments.length;
    }

    @NonNull
    @Override
    public Fragment getItem(int pos){
        return fragments[pos];
    }

}
