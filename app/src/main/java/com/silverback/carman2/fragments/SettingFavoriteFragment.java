package com.silverback.carman2.fragments;


import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.SettingFavoritePagerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.DataProviderContract;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFavoriteFragment extends Fragment implements
        ViewPager.OnPageChangeListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(SettingFavoriteFragment.class);

    // Objects

    // Constructor
    public SettingFavoriteFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View localView = inflater.inflate(R.layout.fragment_favorite_pager, container, false);

        // TabLayout
        TabLayout tabLayout = localView.findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());


        // ViewPager
        ViewPager viewPager = localView.findViewById(R.id.viewpager_favorite);
        SettingFavoritePagerAdapter adapter = new SettingFavoritePagerAdapter(getFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);

        // setupWithViewPager() prevents tab titles from displaying. To avoid this, override
        // onPageTitle() defined in FragmentPagerAdapter.
        tabLayout.setupWithViewPager(viewPager);

        return localView;
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        switch(position) {
            case 0: //favorite gas station
                //loaderManager.initLoader(1, null, this);
                break;
            case 1: //favorite service station
                //loaderManager.initLoader(2, null, this);
                break;
        }
    }
    @Override
    public void onPageSelected(int position) {}
    @Override
    public void onPageScrollStateChanged(int state) {}

}
